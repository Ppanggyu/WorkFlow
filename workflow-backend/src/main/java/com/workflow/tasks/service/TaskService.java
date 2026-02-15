package com.workflow.tasks.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.common.exception.ApiException;
import com.workflow.common.exception.UnauthorizedException;
import com.workflow.department.entity.DepartmentEntity;
import com.workflow.tasks.dto.TaskCreateRequestDTO;
import com.workflow.tasks.dto.TempImageDTO;
import com.workflow.tasks.entity.TasksEntity;
import com.workflow.tasks.enums.Status;
import com.workflow.tasks.enums.Visibility;
import com.workflow.tasks.repository.TaskRepository;
import com.workflow.tasks.view.TasksView;
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
	private final String WIN_TEMP_DIR = "C:/WorkFlow/";

	public Page<TasksView> tasks(Long id, String filter, Pageable pageable, Status selecteStatus) {
		Page<TasksView> taskList = null;
		UserEntity user = userRepository.findById(id).orElseThrow(() -> new UnauthorizedException("오류"));

		switch ((selecteStatus == null) && filter != null ? filter : "all") {
		case "company":
			taskList = taskRepository.findByIsDeletedFalseAndVisibility(Visibility.PUBLIC, pageable);
			break;
		case "myDepartment":
			taskList = taskRepository.findByIsDeletedFalseAndWorkDepartmentId(user.getDepartmentId(), pageable);
			break;
		case "create":
			taskList = taskRepository.findTasksByIsDeletedFalseAndCreatedById(id, pageable);
			break;
		case "assignee":
			taskList = taskRepository.findByIsDeletedFalseAndAssigneeId(user, pageable);
			break;
		case "all":
		default:
//			taskList = taskRepository.findAllByIsDeletedFalse(pageable);
			break;
		}
		
		if(selecteStatus != null && filter == null) {
//			selecteStatus 얘만 있을때
//			filter 얘만 있을때
//
//			selecteStatus filter 둘 다 있을 때 20260215 멈춤
		}

		return taskList;
	}

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
		String updatedDescription;
		
		if(!taskCreateRequestDTO.tempImages().isEmpty()) {
			moveTempImages(taskCreateRequestDTO.tempImages(), newTaskId);
			Path tempFilePath = Paths.get(taskCreateRequestDTO.tempImages().get(0).path()); // temp/uuid폴더/uuid파일
			String uuidFolderName = tempFilePath.getParent().getFileName().toString(); // uuid폴더
			updatedDescription = task.getDescription()
					.replace("/temp/" + uuidFolderName,
							"/" + newTaskId);
			task.setDescription(updatedDescription);
			taskRepository.save(task);
		}
		
	}
	
	public TasksView taskSelected(Long taskId){
		TasksView selected = taskRepository.findProjectedById(taskId)
				.orElseThrow(() -> new RuntimeException("없음"));
		return selected;
	}

	// 이미지 업로드
	public String imageUpload(MultipartFile file, String uuid, HttpServletRequest req) {

		if (file.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_FILE_EMPTY", "업로드할 파일이 없습니다.");
		}

		String originalFileName = file.getOriginalFilename();
		String extension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase(); // 대문자 -> 소문자
		System.out.println("originalFileName : " + originalFileName);
		System.out.println("extension : " + extension);

		Set<String> allowExtensions = Set.of(".gif", ".jpg", ".png", ".jpeg", "webp");
		if (!allowExtensions.contains(extension)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_FILE_EXTENSION",
					"잘못된 파일 입니다. 사용가능 확장자 : .gif .jpg .png. jpeg");
		}

		Path uploadPath = null;
		Path targetPath = null;
		String imageURL = null;
		try {

			UUID fileUUID = UUID.randomUUID();
			uploadPath = Paths.get(WIN_TEMP_DIR + "/temp/", uuid);
			Files.createDirectories(uploadPath);

			String savedFileName = fileUUID + extension;
			targetPath = uploadPath.resolve(savedFileName);

			file.transferTo(targetPath.toFile()); // 저장

			// replace
			// -> http://localhost:8081/workflow/api/upload 에서 /workflow/api/upload 지움
			imageURL = req.getRequestURL().toString().replace(req.getRequestURI(), "") + "/temp/" + uuid + "/"
					+ savedFileName; // http://localhost:8081/temp/폴더명/파일명

		} catch (IOException e) {
			try {
				Files.deleteIfExists(targetPath); // 파일 삭제
			} catch (IOException e1) {
				throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_DELETE_EXCEPTION", "업로드 실패 : 삭제 실패");
			}
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_EXCEPTION", "업로드 실패");
		}

		return imageURL;
	}
	
	// 이미지 삭제
	public void deleteImage(String path) {
		
		Path targetPath = Paths.get(WIN_TEMP_DIR, path.substring(1));
		
		System.out.println("targetPath : " + targetPath);
		
		try {
			Files.deleteIfExists(targetPath); // 파일 삭제
		} catch (IOException e1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "UPLOAD_DELETE_EXCEPTION", "업로드 실패 : 삭제 실패");
		}
	}
	
	// 임시저장 파일 이동
	public void moveTempImages(List<TempImageDTO> tempImages, Long newTaskId) {
		
		Path tempFilePath = Paths.get(tempImages.get(0).path()); // temp/uuid폴더/uuid파일
		
		String uuidFolderName = tempFilePath.getParent().toString(); // temp/uuid폴더
		
		Path tempPath = Paths.get(WIN_TEMP_DIR, "temp"); // C:\WorkFlow\temp
		Path taskFolder = Paths.get(WIN_TEMP_DIR, newTaskId.toString()); // C:/WorkFlow/게시글번호
		Path tempFolder = Paths.get(WIN_TEMP_DIR, uuidFolderName); // C:/WorkFlow/temp/uuid폴더
		
		if (Files.exists(tempPath) && Files.isDirectory(tempPath)) {
			if(taskFolder.isAbsolute()) {
				try {
					Files.createDirectories(taskFolder); // 게시글 번호에 맞는 파일 생성
				} catch (IOException e) {
					System.out.println("신규 파일 생성 실패");
				}
			}
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(tempFolder)){
				for (Path file : stream) {
					Path targetPath = taskFolder.resolve(file.getFileName()); // 파일 옮길 폴더 위치
					Files.move(file, targetPath, StandardCopyOption.REPLACE_EXISTING); // 옮기기
				}
				Files.deleteIfExists(tempFolder); // temp에 남은 폴더 삭제
			}catch(IOException e) {
				System.out.println("Temp 폴더 못찾음");
			}
		}
		
	}
	

}
