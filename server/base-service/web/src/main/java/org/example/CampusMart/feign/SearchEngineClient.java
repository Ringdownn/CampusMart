package org.example.CampusMart.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "search-engine", url = "${search-engine.url:http://localhost:5678}", path = "/api")
public interface SearchEngineClient {

    @PostMapping("/search")
    Map<String, Object> searchLegacy(@RequestParam("database") String database,
                                     @RequestParam("collection") String collection,
                                     @RequestParam("text") String text,
                                     @RequestParam("page") int page,
                                     @RequestParam("size") int size);

    @PostMapping("/query")
    Map<String, Object> search(@RequestParam("database") String database,
                               @RequestBody Map<String, Object> request);

    @PostMapping("/index")
    Map<String, Object> addIndex(@RequestParam("database") String database,
                                  @RequestParam("collection") String collection,
                                  @RequestBody Map<String, Object> document);

    @PostMapping("/index/batch")
    Map<String, Object> batchAddIndex(@RequestParam("database") String database,
                                       @RequestParam("collection") String collection,
                                       @RequestBody java.util.List<Map<String, Object>> documents);

    @PostMapping("/index/remove")
    Map<String, Object> removeIndex(@RequestParam("database") String database,
                                     @RequestParam("collection") String collection,
                                     @RequestBody Map<String, Object> document);
}
