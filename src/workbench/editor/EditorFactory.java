package workbench.editor;

import java.util.HashMap;
import java.util.Map;

public class EditorFactory {

	private interface EditorCreator {
		Editor create();
	}
	
	private static Map<String, EditorCreator> creators;
	static {
		creators = new HashMap<String, EditorCreator>();
		creators.put(CustomObjectEditor.getType(), () -> new CustomObjectEditor());
	}
	
	public static Editor createEditor(String type) {
		if (creators.containsKey(type)) {
			return creators.get(type).create();
		}
		else {
			return new DefaultEditor();
		}
	}
}
