package org.maggus.myhealthnb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class DummyDTO {
    private String name;
    private int age;
    private Long id;

    private DummyItemDTO item;

    private byte[] bytes;
    private int[] numbers;
    private Long[] bigNumbers;
    private List<Integer> numbersList;
    private Map<String, Integer> numbersMap;

    private DummyItemDTO[] items;
    private List<DummyItemDTO> itemsList;
    private Map<String, DummyItemDTO> itemsMap;

    @Data
    public static class DummyItemDTO {
        private long itemId;
        private String itemName;

        public static DummyItemDTO makeDummyItemDTO() {
            DummyItemDTO item = new DummyItemDTO();
            item.itemId = (long) Math.floor(Math.random() * 10);
            item.itemName = "Item #" + item.itemId;
            return item;
        }
    }

    public static DummyDTO makeDummyDTO(boolean withComposition, boolean withCollections) {
        DummyDTO dto = new DummyDTO();
        dto.name = "Some Name";
        dto.age = 123456;
        dto.id = null;

        if (withComposition) {
            dto.item = DummyItemDTO.makeDummyItemDTO();
        }

        if (withCollections) {
            dto.bytes = "SomeBytes".getBytes(StandardCharsets.UTF_8);
            dto.numbers = new int[]{0, 1, 2, 3};
            dto.bigNumbers = new Long[]{5L, 6L, 7L, 0L};
            dto.numbersList = new ArrayList<Integer>() {{
                add(9);
                add(8);
                add(7);
            }};
            dto.numbersMap = new HashMap<String, Integer>() {{
                put("#4", 4);
                put("#5", 5);
                put("#6", 6);
            }};

            if (withComposition) {
                dto.items = new DummyItemDTO[2];
                dto.items[0] = DummyItemDTO.makeDummyItemDTO();
                dto.items[1] = DummyItemDTO.makeDummyItemDTO();

                dto.itemsList = new ArrayList<DummyItemDTO>() {{
                    add(DummyItemDTO.makeDummyItemDTO());
                    add(DummyItemDTO.makeDummyItemDTO());
                    add(DummyItemDTO.makeDummyItemDTO());
                }};
                dto.itemsMap = new HashMap<String, DummyItemDTO>() {{
                    DummyItemDTO item = DummyItemDTO.makeDummyItemDTO();
                    put(item.itemId + "#", item);
                    item = DummyItemDTO.makeDummyItemDTO();
                    put(item.itemId + "#", item);
                    item = DummyItemDTO.makeDummyItemDTO();
                    put(item.itemId + "#", item);
                }};
            }
        }

        return dto;
    }
}
