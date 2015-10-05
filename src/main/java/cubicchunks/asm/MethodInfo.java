package cubicchunks.asm;

/**
 * Contains deobfuscated and srg method names.
 */
public class MethodInfo {
	private String srg;
	private String deobf;

	public MethodInfo(String srg, String deobf) {
		this.srg = srg;
		this.deobf = deobf;
	}

	public String getSrg() {
		return srg;
	}

	public boolean sameAs(String name) {
		return srg.equals(name) || deobf.equals(name);
	}
}
