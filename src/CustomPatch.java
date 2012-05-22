import org.objectweb.asm.ClassVisitor;

public interface CustomPatch {
	public ClassVisitor create(ClassVisitor parent);
}
