public class TestResources {
    public static void main(String[] args) throws Exception {
        var is1 = net.elytrium.limboapi.LimboAPI.class.getResourceAsStream("/mapping/data_component_types.json");
        System.out.println("data_component_types.json: " + (is1 != null ? "FOUND" : "NULL"));
        var is2 = net.elytrium.limboapi.LimboAPI.class.getResourceAsStream("/mapping/data_component_types_mapping.json");
        System.out.println("data_component_types_mapping.json: " + (is2 != null ? "FOUND" : "NULL"));
        // Also try without leading slash
        var is3 = net.elytrium.limboapi.LimboAPI.class.getResourceAsStream("mapping/data_component_types.json");
        System.out.println("(no slash) data_component_types.json: " + (is3 != null ? "FOUND" : "NULL"));
        // Try classloader
        var is4 = net.elytrium.limboapi.LimboAPI.class.getClassLoader().getResourceAsStream("mapping/data_component_types.json");
        System.out.println("ClassLoader: " + (is4 != null ? "FOUND" : "NULL"));
        System.out.println("ClassLoader: " + net.elytrium.limboapi.LimboAPI.class.getClassLoader());
        System.out.println("Class: " + net.elytrium.limboapi.LimboAPI.class);
    }
}
