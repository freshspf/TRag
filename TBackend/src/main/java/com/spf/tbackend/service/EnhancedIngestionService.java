package com.spf.tbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedIngestionService {

    private final VectorStore vectorStore;

    // 文本分割器配置
    private static final int DEFAULT_CHUNK_SIZE = 1000;
    private static final int DEFAULT_CHUNK_OVERLAP = 200;

    /**
     * 基础文档摄取方法
     */
    public void ingestFile(MultipartFile file) {
        ingestFileWithMetadata(file, new HashMap<>());
    }

    /**
     * 带自定义元数据的文档摄取
     */
    public void ingestFileWithMetadata(MultipartFile file, Map<String, Object> additionalMetadata) {
        Path tempFile = null;
        try {
            // 创建临时文件
            tempFile = createTempFile(file);

            // 创建资源对象
            Resource resource = new UrlResource(tempFile.toUri());

            // 读取文档
            List<Document> documents = readDocuments(resource, file);

            if (documents.isEmpty()) {
                log.warn("文档解析后内容为空: {}", file.getOriginalFilename());
                return;
            }

            // 添加元数据
            documents = addMetadataToDocuments(documents, file, additionalMetadata);

            // 分割文档
            List<Document> splitDocs = splitDocuments(documents);

            // 存储到向量数据库
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
            cleanupTempFile(tempFile);
        }
    }

    /**
     * 批量摄取文档
     */
    public void ingestBatchFiles(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            try {
                ingestFile(file);
            } catch (Exception e) {
                log.error("批量处理中文件失败: {}", file.getOriginalFilename(), e);
                // 可以选择继续处理其他文件或抛出异常
            }
        }
    }

    /**
     * 智能文档摄取（根据文件类型选择不同策略）
     */
    public void ingestFileIntelligently(MultipartFile file) {
        String contentType = file.getContentType();
        Map<String, Object> metadata = new HashMap<>();

        // 根据文件类型添加特定处理
        if (contentType != null) {
            metadata.put("content_type", contentType);

            // 可以根据不同文件类型使用不同的分割策略
            TextSplitter splitter = getTextSplitterForContentType(contentType);
            // 这里可以进一步优化处理逻辑
        }

        ingestFileWithMetadata(file, metadata);
    }

    /**
     * 创建临时文件
     */
    private Path createTempFile(MultipartFile file) throws IOException {
        String uniqueFileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        return Files.createTempFile("ingest_", "_" + uniqueFileName);
    }

    /**
     * 读取文档
     */
    private List<Document> readDocuments(Resource resource, MultipartFile file) throws IOException {
        file.transferTo(resource.getFile().toPath());

        // 使用Tika文档读取器
        DocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }

    /**
     * 为文档添加元数据
     */
    private List<Document> addMetadataToDocuments(List<Document> documents,
                                                MultipartFile file,
                                                Map<String, Object> additionalMetadata) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // 基础元数据
        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put("filename", file.getOriginalFilename());
        baseMetadata.put("original_filename", file.getOriginalFilename());
        baseMetadata.put("content_type", file.getContentType());
        baseMetadata.put("file_size", file.getSize());
        baseMetadata.put("ingestion_timestamp", timestamp);
        baseMetadata.put("document_id", UUID.randomUUID().toString());

        // 合并额外元数据
        baseMetadata.putAll(additionalMetadata);

        // 为每个文档添加元数据
        return documents.stream()
                .map(doc -> {
                    Map<String, Object> docMetadata = new HashMap<>(baseMetadata);
                    docMetadata.putAll(doc.getMetadata());
                    return new Document(doc.getId(), doc.getContent(), docMetadata);
                })
                .toList();
    }

    /**
     * 分割文档
     */
    private List<Document> splitDocuments(List<Document> documents) {
        // TokenTextSplitter构造参数：
        // defaultChunkSize: 默认块大小
        // minChunkSizeChars: 最小块字符数
        // minChunkLengthToEmbed: 嵌入的最小块长度
        // maxNumChunks: 最大块数量
        // keepSeparator: 是否保留分隔符
        TokenTextSplitter splitter = new TokenTextSplitter(
            DEFAULT_CHUNK_SIZE,    // 默认块大小
            DEFAULT_CHUNK_OVERLAP, // 最小块字符数（用作重叠）
            5,                     // 嵌入的最小块长度
            10000,                 // 最大块数量
            true                   // 保留分隔符
        );
        return splitter.apply(documents);
    }

    /**
     * 根据内容类型选择文本分割器
     */
    private TextSplitter getTextSplitterForContentType(String contentType) {
        // 可以根据不同文件类型返回不同的分割器
        if (contentType != null && contentType.startsWith("text/")) {
            // 对于纯文本，可以使用更大的chunk
            return new TokenTextSplitter(2000, 400, 5, 10000, true);
        } else if (contentType != null && contentType.contains("pdf")) {
            // 对于PDF，可能需要不同的策略
            return new TokenTextSplitter(800, 150, 5, 10000, true);
        }

        // 默认分割器
        return new TokenTextSplitter();
    }

    /**
     * 清理临时文件
     */
    private void cleanupTempFile(Path tempFile) {
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