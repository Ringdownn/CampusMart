package org.example.CampusMart.feign;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.CampusMart.model.entity.Goods;
import org.example.CampusMart.model.entity.Picture;
import org.example.CampusMart.web.mapper.GoodsMapper;
import org.example.CampusMart.web.mapper.PictureMapper;
import org.example.CampusMart.web.mapper.UserMapper;
import org.example.CampusMart.web.vo.GoodsVo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SearchEngineService {

    private static final String DATABASE_NAME = "campusmart";
    private static final String COLLECTION_NAME = "goods";
    private static final String INDEX_EXCHANGE = "index_exchange";
    private static final String INDEX_ROUTING_KEY_PREFIX = "index.update";
    private static final String INDEX_SOURCE = "base";

    @Autowired
    private SearchEngineClient searchEngineClient;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private PictureMapper pictureMapper;

    @Autowired
    private UserMapper userMapper;

    public IPage<GoodsVo> searchGoods(String keyword, long current, long size) {
        Map<String, Object> request = new HashMap<>();
        request.put("query", keyword);
        request.put("page", (int) current);
        request.put("limit", (int) size);

        Map<String, Object> result = searchEngineClient.search(DATABASE_NAME, request);
        Map<String, Object> data = result != null ? (Map<String, Object>) result.get("data") : null;
        List<Map<String, Object>> documents = data != null ? (List<Map<String, Object>>) data.get("documents") : null;

        List<GoodsVo> goodsVoList = new ArrayList<>();

        if (documents != null) {
            for (Map<String, Object> hit : documents) {
                Map<String, Object> doc = (Map<String, Object>) hit.get("document");
                if (doc != null) {
                    GoodsVo vo = new GoodsVo();
                    vo.setGoodID(((Number) doc.get("goodID")).longValue());
                    vo.setPublishUserID(((Number) doc.get("publishUserID")).longValue());
                    vo.setTitle((String) doc.get("title"));
                    vo.setAppearance((String) doc.get("appearance"));
                    vo.setItemDescription((String) doc.get("itemDescription"));
                    vo.setPrice(((Number) doc.get("price")).longValue());
                    vo.setPictureURL((String) doc.get("pictureURL"));
                    vo.setNickname((String) doc.get("nickname"));
                    vo.setAvatarURL((String) doc.get("avatarURL"));
                    goodsVoList.add(vo);
                }
            }
        }

        long total = data != null && data.get("total") instanceof Number ? ((Number) data.get("total")).longValue() : goodsVoList.size();
        Page<GoodsVo> page = new Page<>(current, size);
        page.setTotal(total);
        page.setRecords(goodsVoList);

        return page;
    }

    public void addIndex(Long goodsId) {
        Map<String, Object> indexDoc = buildIndexDocument(goodsId);
        searchEngineClient.addIndex(DATABASE_NAME, COLLECTION_NAME, indexDoc);
    }

    public void updateIndex(Long goodsId) {
        Map<String, Object> indexDoc = buildIndexDocument(goodsId);
        searchEngineClient.addIndex(DATABASE_NAME, COLLECTION_NAME, indexDoc);
    }

    public void removeIndex(Long goodsId) {
        Map<String, Object> document = new HashMap<>();
        document.put("id", buildSearchIndexId(goodsId));
        searchEngineClient.removeIndex(DATABASE_NAME, COLLECTION_NAME, document);
    }

    public void publishIndexUpdate(Long goodsId) {
        Map<String, Object> indexTask = buildIndexTask(goodsId);
        if (indexTask.isEmpty()) {
            return;
        }
        rabbitTemplate.convertAndSend(INDEX_EXCHANGE, buildIndexRoutingKey(INDEX_SOURCE), indexTask);
    }

    public Map<String, Object> buildIndexTask(Long goodsId) {
        Map<String, Object> indexDoc = buildIndexDocument(goodsId);
        if (indexDoc.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> task = new HashMap<>();
        task.put("database", DATABASE_NAME);
        task.put("doc", indexDoc);
        task.put("source", INDEX_SOURCE);
        return task;
    }

    public Map<String, Object> buildIndexDocument(Long goodsId) {
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) {
            return new HashMap<>();
        }

        LambdaQueryWrapper<Picture> pictureQuery = new LambdaQueryWrapper<>();
        pictureQuery.eq(Picture::getGoodID, goods.getGoodID()).last("LIMIT 1");
        Picture picture = pictureMapper.selectOne(pictureQuery);
        String pictureURL = picture != null ? picture.getPictureURL() : "";

        Map<String, Object> userInfo = goodsMapper.selectUserInfo(goods.getPublishUserID());

        Map<String, Object> document = new HashMap<>();
        document.put("id", goods.getGoodID());
        document.put("goodID", goods.getGoodID());
        document.put("publishUserID", goods.getPublishUserID());
        document.put("title", goods.getTitle());
        document.put("appearance", goods.getAppearance());
        document.put("itemDescription", goods.getItemDescription());
        document.put("price", goods.getPrice());
        document.put("pictureURL", pictureURL);
        document.put("nickname", userInfo != null ? userInfo.get("nickname") : "");
        document.put("avatarURL", userInfo != null ? userInfo.get("avatarURL") : "");

        Map<String, Object> indexDoc = new HashMap<>();
        indexDoc.put("id", buildSearchIndexId(goods.getGoodID()));
        indexDoc.put("text", goods.getTitle() + " " + goods.getItemDescription());
        if (pictureURL != null && !pictureURL.isEmpty()) {
            indexDoc.put("imageURL", pictureURL);
        }
        indexDoc.put("document", document);

        return indexDoc;
    }

    private String buildIndexRoutingKey(String source) {
        return INDEX_ROUTING_KEY_PREFIX + "." + source;
    }

    private long buildSearchIndexId(Long goodsId) {
        return Integer.toUnsignedLong(Long.hashCode(goodsId));
    }
}
