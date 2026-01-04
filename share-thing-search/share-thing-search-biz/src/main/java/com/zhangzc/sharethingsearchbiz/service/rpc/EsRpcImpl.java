package com.zhangzc.sharethingsearchbiz.service.rpc;

import com.zhangzc.sharethingsearchapi.pojo.req.EsArticleDto;
import com.zhangzc.sharethingsearchapi.rpc.EsRpc;
import com.zhangzc.sharethingsearchbiz.esMapper.EsArticleMapper;
import com.zhangzc.sharethingsearchbiz.esPojo.EsArticle;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.dromara.easyes.core.conditions.select.LambdaEsQueryWrapper;
import org.springframework.beans.BeanUtils;

import java.util.List;

@DubboService
@RequiredArgsConstructor
public class EsRpcImpl implements EsRpc {
    private final EsArticleMapper esArticleMapper;

    @Override
    public Long addArticles(List<EsArticleDto> esArticleDto) {
        try {
            List<EsArticle> list = esArticleDto.stream().map(esArticleDto1 -> {
                EsArticle esArticle = new EsArticle();
                BeanUtils.copyProperties(esArticleDto1, esArticle);
                return esArticle;
            }).toList();
            Integer i = esArticleMapper.insertBatch(list);
            return Long.valueOf(i.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<EsArticleDto> search(String keyword) {
        LambdaEsQueryWrapper<EsArticle> queryWrapper = new LambdaEsQueryWrapper<>();
        queryWrapper
                .like(EsArticle::getTitle, keyword)
                .or()
                .like(EsArticle::getSummary, keyword)
                .eq(EsArticle::getIsDeleted, false)
                .eq(EsArticle::getState, 1)
                .sortByScore(true)
        ;
        List<EsArticle> list = esArticleMapper.selectList(queryWrapper);
        return list.stream().map(esArticle -> {
            EsArticleDto esArticleDto = new EsArticleDto();
            BeanUtils.copyProperties(esArticle, esArticleDto);
            return esArticleDto;
        }).toList();
    }
}
