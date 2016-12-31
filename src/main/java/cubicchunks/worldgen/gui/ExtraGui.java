package cubicchunks.worldgen.gui;

import net.malisis.core.client.gui.MalisisGui;
import net.malisis.core.client.gui.component.UIComponent;
import net.malisis.core.util.MouseButton;

import org.lwjgl.input.Mouse;

import java.util.Map;
import java.util.WeakHashMap;

import cubicchunks.worldgen.gui.component.IDragTickable;

public abstract class ExtraGui extends MalisisGui {

	private Map<IDragTickable, DragTickableWrapper> set = new WeakHashMap<>();

	@Override
	public void update(int mouseX, int mouseY, float partialTick) {
		set.values().forEach(tc -> tc.tick(mouseX, mouseY, partialTick));
	}

	public <T extends UIComponent<X> & IDragTickable, X extends UIComponent<X>> void registerDragTickable(T t) {
		set.put(t, new DragTickableWrapper(t));
	}

	public static final class DragTickableWrapper {
		private final IDragTickable component;
		private boolean beforeClickHovered = false;

		public DragTickableWrapper(IDragTickable component) {
			this.component = component;
		}

		void tick(int mouseX, int mouseY, float partialTick) {
			if (((UIComponent<?>) component).isFocused() && beforeClickHovered && Mouse.isButtonDown(MouseButton.LEFT.getCode()))
				component.onDragTick(mouseX, mouseY, partialTick);
			else
				beforeClickHovered = ((UIComponent<?>) component).isHovered();
		}
	}
}
