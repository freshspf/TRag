package com.spf.tbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;

    public void ingestFile(MultipartFile file) {
        Path tempFile = null;
        try {
            // 生成唯一的临时文件名避免冲突
            String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            tempFile = Files.createTempFile("ingest_", "_" + uniqueFileName);

            // 保存文件到临时位置
            file.transferTo(tempFile);

            // 创建资源对象
            Resource resource = new UrlResource(tempFile.toUri());

            // 使用Tika读取文档
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            // 如果文档为空，记录警告并返回
            if (documents.isEmpty()) {
                log.warn("文档解析后内容为空: {}", file.getOriginalFilename());
                return;
            }

            // 配置文本分割器（可以根据需要调整参数）
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            // 添加到向量存储
            vectorStore.add(splitDocs);

            log.info("文档处理成功 - 文件: {}, 原始文档数: {}, 分割后文档数: {}",
                    file.getOriginalFilename(), documents.size(), splitDocs.size());

        } catch (IOException e) {
            log.error("文件IO操作失败 - 文件: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件处理失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("文档处理过程中发生未知错误 - 文件: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("文档处理失败: " + e.getMessage(), e);
        } finally {
            // 清理临时文件
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                    log.debug("临时文件已删除: {}", tempFile);
                } catch (IOException e) {
                    log.warn("删除临时文件失败: {}", tempFile, e);
                }
            }
        }
    }
}