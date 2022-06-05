import com.fasterxml.jackson.core.JsonProcessingException;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Test {

    public static void testMapper() throws JsonProcessingException {
        Map<String, Object> finalMap = new HashMap<>();
        List<Map<String,String>> list = new ArrayList<>();
        Map<String, String> localMap = new HashMap<>();
        localMap.put("dev_id", "conditioner");
        list.add(localMap);
        finalMap.put("near_dev", list);
        finalMap.put("timestamp", System.currentTimeMillis());
        System.out.println(Utils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(finalMap));
    }

    public static void main(String[] args) {
        try {
            testMapper();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }
}
