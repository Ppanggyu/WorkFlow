package com.workflow.tasks.autoClean;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TempCleaner {
	
	private final String WIN_TEMP_DIR = "C:/WorkFlow/";
	
	@Scheduled(fixedRate = 12 * 60 * 60 * 1000) // ms 단위
	public void cleanOldFiles() {
		
		Path path = Paths.get(WIN_TEMP_DIR, "temp");
		
		Instant cutOff = Instant.now().minusSeconds(12 * 60 * 60); // 초 단위
		
		if (!Files.exists(path) || !Files.isDirectory(path)) {
			System.out.println("temp가 없음");
			return;
		}
		
		// 경로 안에 있는 모든 항목(폴더/파일) 처리하기 위해 디렉토리 스트림 열기
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(path)){
			// 디렉토리 스트림 안의 각각 항목 반복
			for (Path folder : stream) {
				// 현재 항목이 폴더인지 확인
				if(Files.isDirectory(folder)) {
					try {
						// 폴더 속성 확인 : 생성 시간
						BasicFileAttributes attr = Files.readAttributes(folder, BasicFileAttributes.class);
						Instant creationTime = attr.creationTime().toInstant();
						
						// 현재 시간 기준 12시간 이상된 폴더
						if(creationTime.isBefore(cutOff)) {
							// 폴더 안의 파일과 하위 폴더를 탐색
							Files.walk(folder)
							// 하위부터 삭제
							.sorted((a, b) -> b.compareTo(a))
							// 각각 파일 폴더 삭제 시도
							.forEach(path1 -> {
								try {
									Files.deleteIfExists(path1);
									System.out.println("삭제 : " + path1);
								}catch(IOException e) {
									System.out.println("실패 : " + path1);
								}
							});
						}
					} catch(IOException e) {
						System.out.println("폴더 속성 읽기 실패 : " + folder);
					}
				}
			}
		}catch(IOException e) {
			System.out.println("Temp 폴더 탐색 실패 : " + path);
		}
		
	}

}
