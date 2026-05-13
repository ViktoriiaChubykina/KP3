package lab.model;

import java.util.*;

public class Flowchart {
    private String name = "Thread-1";
    private String startBlockId;
    private final List<Block> blocks = new ArrayList<>();

    public Flowchart() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "Thread";
    }

    public String getStartBlockId() {
        return startBlockId;
    }

    public void setStartBlockId(String startBlockId) {
        this.startBlockId = startBlockId;
    }

    public List<Block> getBlocks() {
        return blocks;
    }

    public Optional<Block> find(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return blocks.stream().filter(b -> id.equals(b.getId())).findFirst();
    }

    public void removeBlock(String blockId) {
        blocks.removeIf(b -> Objects.equals(blockId, b.getId()));
        blocks.forEach(b -> {
            Iterator<Map.Entry<String, String>> it = b.getOutgoing().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String> e = it.next();
                if (Objects.equals(e.getValue(), blockId)) {
                    it.remove();
                }
            }
        });
        if (Objects.equals(blockId, startBlockId)) {
            startBlockId = null;
        }
    }
}
