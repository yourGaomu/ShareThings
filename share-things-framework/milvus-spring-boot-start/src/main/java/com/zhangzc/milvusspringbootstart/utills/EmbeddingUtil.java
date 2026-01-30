package com.zhangzc.milvusspringbootstart.utills;

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.milvusspringbootstart.core.service.SliceService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingUtil {
    private final EmbeddingModel embeddingModel;
    private final SliceService sliceService;

    public List<Float> embed(Object object) {
        return embeddingModel.embed(JsonUtils.toJsonString(object)).content().vectorAsList();
    }

    /**
    * 只有当选择了嵌入mode是根据自定义大小，第二个函数才会生效
    * */
    public List<Float> embed(String text, int size) {
        List<Float> floats = null;
        try {
            //进行切片操作
            List<String> slice = sliceService.slice(text, size);
            List<TextSegment> list = slice.stream().map(TextSegment::from).toList();
            Response<List<Embedding>> listResponse = embeddingModel.embedAll(list);
            listResponse.content().forEach(embedding -> {
                List<Float> floats1 = embedding.vectorAsList();
                if (floats != null) {
                    floats.addAll(floats1);
                }
            });
        } catch (Exception e) {
            log.error(e.getMessage());
            throw e;
        }
        return floats;
    }

    /**
    * 普通的文字列表嵌入
    * */
    public List<List<Float>> embed(List<String> texts) {
        List<List<Float>> result = new ArrayList<>();
        texts.forEach(text -> {
            List<Float> floats = embeddingModel.embed(text).content().vectorAsList();
            result.add(floats);
        });
        return result;
    }
}
