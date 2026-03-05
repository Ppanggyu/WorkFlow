package com.workflow.tasks.service;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachments.dto.AttachmentsDTO;
import com.workflow.attachments.dto.TempFileDTO;
import com.workflow.attachments.entity.AttachmentsEntity;
import com.workflow.attachments.repository.AttachmentsRepository;
import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.dto.TaskDTO;
import com.workflow.tasks.dto.TaskFilesDTO;
import com.workflow.tasks.dto.TaskSelectedRes;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.user.entity.UserEntity;
import com.workflow.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
//final이거나 @NonNull이 붙은 필드만 파라미터로 받는 생성자를 자동 생성
@RequiredArgsConstructor
@Transactional
public class TaskService {

	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final AttachmentsRepository attachmentsRepository;
	private final String WIN_TEMP_DIR = "C:/WorkFlow/";
	
	// task 목록 조회 : 전체, 필터별 조회 / 페이징
	@Transactional(readOnly = true)
	public Page<TaskDTO> tasks(Long id, String filter, Pageable pageable, Status selecteStatus) {
		UserEntity user = userRepository.findById(id).orElseThrow(() -> new UnauthorizedException("오류"));
		
		// 조회 시 기본 조건 : 논리삭제되지 않은 게시글
		// Specification : WHERE 조건을 동적 쿼리 생성
		// 사용 환경 조건 : 1. 검색 필터 여러개인 경우 2. 조건 조합이 많은 경우 3. 목록 조회
		// 사용 금지 조건 : 1. 단순 조회 2. 성능이 매우 중요한 대량 트래픽 핵심 API
		Specification<TasksEntity> spec = Specification.allOf(isNotDeleted());
		
		// 전체, 전사, 우리팀, 내가만든, 담당 업무 필터
		if (filter != null) {
			switch (filter) {
			case "company":
				// root : Entity 필드 접근 객체
				// spec.and() : 기존 조건에 AND를 추가하는 역할
				// SQL로 변환 시
				// cb.equal : =, cb.like : LIKE, cb.greaterThan : >, cb.lessThan : <, cb.and : AND, cb.or : or
				spec = spec.and((root, query, cb) -> cb.equal(root.get("visibility"), Visibility.PUBLIC));
				break;
			case "myDepartment":
				spec = spec.and((root, query, cb) -> cb.equal(root.get("workDepartmentId").get("id"),
						user.getDepartmentId().getId()));
				break;
			case "create":
				spec = spec.and((root, query, cb) -> cb.equal(root.get("createdBy").get("id"), id));
				break;
			case "assignee":
				spec = spec.and((root, query, cb) -> cb.equal(root.get("assigneeId").get("id"), id));
				break;
			}
		}

		if (selecteStatus != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), selecteStatus));
		}
		
		Page<TasksEntity> entityPage = taskRepository.findAll(spec, pageable);
		
		return entityPage.map(TaskDTO::toDto);
	}
	
	// task 작성
	public void taskForm(TaskCreateRequestDTO taskCreateRequestDTO, Long userId) {

		// 작성자 조회
		UserEntity creator = userRepository.findById(userId)
				.orElseThrow(() -> new UnauthorizedException("사용자를 찾을 수 없습니다."));

		// 담당자 조회
		UserEntity assignee = userRepository.findById(taskCreateRequestDTO.assigneeId())
				.orElseThrow(() -> new RuntimeException("담당자를 찾을 수 없습니다."));

		// 부서 조회
		DepartmentEntity createrDepartment = creator.getDepartmentId();
		// 담당 부서 조회
		DepartmentEntity assigneeDepartment = assignee.getDepartmentId();

		// Entity 생성
		TasksEntity task = TasksEntity.builder().title(taskCreateRequestDTO.title())
				.description(taskCreateRequestDTO.description()).status(Status.TODO) // 생성 시 기본값
				.priority(taskCreateRequestDTO.priority()).visibility(taskCreateRequestDTO.visibility())
				.dueDate(taskCreateRequestDTO.dueDate()).isDeleted(false).createdBy(creator).assigneeId(assignee)
				.ownerDepartmentId(createrDepartment).workDepartmentId(assigneeDepartment).build();

		// 1. save 두번해도 서버 부하는 없는 수준 + 직관적
		// 2. saveAndFlush + save 쓰면 id를 즉시 DB에서 보장받지만 대량 처리시 부하 증가
		taskRepository.save(task);
		Long newTaskId = task.getId();
		String updatedDescription = task.getDescription();
		
		// tempImage, file 존재 확인
		if (!taskCreateRequestDTO.tempImages().isEmpty() || !taskCreateRequestDTO.tempFiles().isEmpty()) {
			
			// temp폴더 내 파일 이동
			moveTempImages(taskCreateRequestDTO, newTaskId);
			
			// tempFile 존재 시 DB저장용
			if (!taskCreateRequestDTO.tempFiles().isEmpty()) {
				List<AttachmentsEntity> listEntity = new ArrayList<>();
				for (TempFileDTO tempFile : taskCreateRequestDTO.tempFiles()) {
					try {
						Path tempFilePath = Paths.get(tempFile.path()); // 전체경로
						String uuidFileEncodingName = tempFilePath.getFileName().toString(); // 파일명
						// UTF-8로 로컬에 있는 파일명 가져오기 - 안할 시 컴퓨터 언어로 가져옴 / 컴퓨터는 읽지만 우리 입장에선 못읽음
						String uuidFileName = URLDecoder.decode(uuidFileEncodingName, StandardCharsets.UTF_8);
						// temp 파일 uuid_원본파일명으로 저장해서 _ 기준으로 자름
						// 앞에서부터 읽어서 자름 : uuid는 _이 안나옴
						String orginalFileName = uuidFileName.contains("_")
								? uuidFileName.substring(uuidFileName.indexOf("_") + 1)
								: uuidFileName;
						
						// 파일이 들어있는 폴더 경로
						Path storagePath = Paths.get(WIN_TEMP_DIR, String.valueOf(newTaskId), "file");
						// 폴더 + 파일 경로
						Path movePath = Paths.get(WIN_TEMP_DIR, String.valueOf(newTaskId), "file", uuidFileName);
						// 파일 사이즈
						long fileSize = Files.size(movePath);

						AttachmentsEntity entity = AttachmentsEntity.builder().taskId(task).uploaderId(creator)
								.originalFilename(orginalFileName).storedFilename(uuidFileName)
								.contentType(Files.probeContentType(tempFilePath).toString())
								.sizeBytes(fileSize).storagePath(storagePath.toString()).isDeleted(false)
								.build();

						listEntity.add(entity);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				attachmentsRepository.saveAll(listEntity);
			}
			
			// tempImage 존재 시 task의 description내용 변경
			if (!taskCreateRequestDTO.tempImages().isEmpty()) {
				Path tempFilePath = Paths.get(taskCreateRequestDTO.tempImages().get(0).path()); // temp/uuid폴더/uuid파일
				String uuidFolderName = tempFilePath.getParent().getFileName().toString(); // uuid폴더
				updatedDescription = task.getDescription().replace("/temp/" + uuidFolderName, "/" + newTaskId); // temp 자르고 newTaskId붙임
			}
			// 최종적으로 <img src="배포 기본 url/폴더명/파일명" 으로 들어감>
			task.setDescription(updatedDescription);
			taskRepository.save(task);
		}
	}
	
	// task 상세 조회
	public TaskSelectedRes taskSelected(Long taskId) {
		
		TasksEntity table = taskRepository.findById(taskId).orElseThrow(() -> new RuntimeException("없음"));
		TaskDTO selected = TaskDTO.toDto(table);
		
		List<AttachmentsDTO> selectedAtt = attachmentsRepository.findByTaskId_Id(selected.id())
				.stream().map(entity -> AttachmentsDTO.from(entity)).toList();
		
		TaskSelectedRes seletedRes = new TaskSelectedRes(selected, selectedAtt);

		return seletedRes;
	}

	// 임시 업로드
	public Map<String, Object> fileUpload(List<MultipartFile> file, String uuid, HttpServletRequest req) {

		if (file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_FILE_EMPTY", "업로드할 파일이 없습니다.");
		}

		List<TaskFilesDTO> taskFiles = new ArrayList<TaskFilesDTO>();

		for (MultipartFile mf : file) {
			
			// 요청 URL 경로 확인
			String stringUrl = String.valueOf(req.getRequestURL());
			String lastUrl = stringUrl.substring(stringUrl.lastIndexOf("/"));
			String originalFileName = mf.getOriginalFilename();
			// 확장자 대문자 -> 소문자
			String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
			
			// 이미지 업로드 시 확장자 필터
			if (!lastUrl.equals("/fileUpload")) {
				Set<String> allowExtensions = Set.of(".gif", ".jpg", ".png", ".jpeg", "webp");
				if (!allowExtensions.contains(extension)) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_FILE_EXTENSION",
							"잘못된 파일 입니다. 사용가능 확장자 : .gif .jpg .png. jpeg");
				}
			}

			Path uploadPath = null;
			Path targetPath = null;
			String imageURL = null;
			try {
				UUID fileUUID = UUID.randomUUID();
				if (lastUrl.equals("/fileUpload")) {
					// 파일업로드 경로
					uploadPath = Paths.get(WIN_TEMP_DIR, "temp", uuid, "file");
				} else {
					// 이미지업로드 경로
					uploadPath = Paths.get(WIN_TEMP_DIR, "temp", uuid);
				}
				// 폴더 생성
				Files.createDirectories(uploadPath);
				// 이미지 파일 저장 uuid.확장자
				String savedFileName = fileUUID + extension;
				// 첨부파일 저장 uuid_파일명.확장자
				String savedFileName2 = fileUUID + "_" + originalFileName;

				// replace
				// -> http://localhost:8081/workflow/api/upload 에서 /workflow/api/upload 지움
				if (lastUrl.equals("/fileUpload")) {
					targetPath = uploadPath.resolve(savedFileName2);
					mf.transferTo(targetPath.toFile()); // 저장

					imageURL = req.getRequestURL().toString().replace(req.getRequestURI(), "") + "/temp/" + uuid
							+ "/file/" + savedFileName2; // http://localhost:8081/temp/폴더명/file/파일명
				}
				if (lastUrl.equals("/imageUpload")) {
					targetPath = uploadPath.resolve(savedFileName);
					mf.transferTo(targetPath.toFile()); // 저장

					imageURL = req.getRequestURL().toString().replace(req.getRequestURI(), "") + "/temp/" + uuid + "/"
							+ savedFileName; // http://localhost:8081/temp/폴더명/uuid_originalFileName
				}

			} catch (IOException e) {
				try {
					Files.deleteIfExists(targetPath); // 오류 시 파일 삭제
				} catch (IOException e1) {
					throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_DELETE_EXCEPTION", "업로드 실패 : 삭제 실패");
				}
				throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_EXCEPTION", "업로드 실패");
			}
			// 저장 경로 출력용 및 파일명
			taskFiles.add(new TaskFilesDTO(imageURL, originalFileName));
		}
		
		Map<String, Object> maps = new HashMap<>();
		maps.put("taskFiles", taskFiles);

		return maps;
	}

	// 이미지 삭제
	public void fileDelete(String path) {

		Path targetPath = Paths.get(WIN_TEMP_DIR, path.substring(1));

		System.out.println("targetPath : " + targetPath);

		try {
			Files.deleteIfExists(targetPath); // 파일 삭제
		} catch (IOException e1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_DELETE_EXCEPTION", "삭제 실패");
		}
	}

	// 임시저장 파일 이동
	public void moveTempImages(TaskCreateRequestDTO taskCreateRequestDTO, Long newTaskId) {
		Path tempFilePath;
		if (!taskCreateRequestDTO.tempImages().isEmpty()) {
			tempFilePath = Paths.get(taskCreateRequestDTO.tempImages().get(0).path()); // temp/uuid폴더/uuid파일
		} else {
			tempFilePath = Paths.get(taskCreateRequestDTO.tempFiles().get(0).path()); // temp/uuid폴더/file/uuid파일
		}

		String uuidFolderName = tempFilePath.getParent().toString(); // temp/uuid폴더

		Path tempPath = Paths.get(WIN_TEMP_DIR, "temp"); // C:\WorkFlow\temp
		Path taskFolder = Paths.get(WIN_TEMP_DIR, newTaskId.toString()); // C:/WorkFlow/게시글번호
		Path taskFileFolder = Paths.get(WIN_TEMP_DIR, newTaskId.toString(), "file"); // C:/WorkFlow/게시글번호/file
		Path tempFolder = Paths.get(WIN_TEMP_DIR, uuidFolderName); // C:/WorkFlow/temp/uuid폴더

		if (Files.exists(tempPath) && Files.isDirectory(tempPath)) {
			// 루트 기준 절대 경로 여부
			if (taskFolder.isAbsolute()) {
				try {
					Files.createDirectories(taskFolder); // 게시글 번호에 맞는 폴더 생성
					Files.createDirectories(taskFileFolder); // 게시글번호 폴더안에 file 폴더 생성
				} catch (IOException e) {
					System.out.println("신규 파일 생성 실패");
				}
			}
			// tempFolder 안에 파일 목록 읽기
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(tempFolder)) {
				if (!taskCreateRequestDTO.tempImages().isEmpty()) {
					for (Path file : stream) {
						Path targetPath = taskFolder.resolve(file.getFileName()); // 파일 옮길 폴더 위치
						Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING); // 옮기기
					}
				}
				if (!taskCreateRequestDTO.tempFiles().isEmpty()) {
					for (Path file : stream) {
						Path targetPath = taskFileFolder.resolve(file.getFileName()); // 파일 옮길 폴더 위치
						Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING); // 옮기기
					}
				}
				Files.deleteIfExists(tempFolder); // temp에 남은 폴더 삭제
			} catch (IOException e) {
				System.out.println("Temp 폴더 못찾음");
			}
		}

	}
	
	// task 목록 조회 메서드에서 사용
	private Specification<TasksEntity> isNotDeleted() {
		return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
	}

}
