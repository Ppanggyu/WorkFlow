package com.workflow.attachments.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.workflow.attachments.entity.AttachmentsEntity;
import com.workflow.attachments.repository.AttachmentsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AttachmentsService {
	
	private final AttachmentsRepository attachmentsRepository;
	
	public void saveAttachments(Long userId, List<MultipartFile> files) {
		
//		List<AttachmentsEntity> listEntity = new ArrayList<>();
//		AttachmentsEntity entity = null;
//		
//		for(MultipartFile mf : files) {
//			entity.builder().storedFilename(mf.getName()).contentType(mf.getContentType())
//			.sizeBytes(mf.getSize()).isDeleted(false).build();
//			
//			listEntity.add(entity);
//		}
//		
//		attachmentsRepository.saveAll(listEntity);
	}

}
