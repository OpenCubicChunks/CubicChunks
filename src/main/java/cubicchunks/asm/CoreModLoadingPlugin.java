package cubicchunks.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.MCVersion(value = "1.8")
@IFMLLoadingPlugin.SortingIndex(value = 5000)
@IFMLLoadingPlugin.TransformerExclusions(value = "cubicchunks.asm.")
public class CoreModLoadingPlugin implements IFMLLoadingPlugin {
	@Override
	public String[] getASMTransformerClass() {
		return new String[]{"cubicchunks.asm.WorldTransformer"};
	}

	@Override
	public String getModContainerClass() {
		return null;
	}

	@Override
	public String getSetupClass() {
		return null;
	}

	@Override
	public void injectData(Map<String, Object> data) {

	}

	@Override
	public String getAccessTransformerClass() {
		return null;
	}
}
