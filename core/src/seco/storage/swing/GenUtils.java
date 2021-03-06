package seco.storage.swing;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JTextField;
import org.hypergraphdb.HGException;
import org.hypergraphdb.HGHandleFactory;
import org.hypergraphdb.HGLink;
import org.hypergraphdb.HGPlainLink;
import org.hypergraphdb.HGValueLink;
import org.hypergraphdb.HyperGraph;
import org.hypergraphdb.type.BonesOfBeans;
import org.hypergraphdb.type.Record;
import org.hypergraphdb.type.Slot;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import seco.storage.swing.types.AddOnFactory;
import seco.storage.swing.types.ClassGenerator;
import seco.storage.swing.types.ConstructorLink;
import seco.storage.swing.types.FactoryConstructorLink;
import seco.storage.swing.types.GeneratedClass;
import seco.storage.swing.types.SwingType;

public class GenUtils
{
	private final static Map<Class<?>, String> DESCRIPTORS;
	private final static Map<Class<?>, Method> METHODS;
	private final static Map<Class<?>, Type> WRAP_TYPES;
	final static Type BYTE_TYPE = Type.getObjectType("java/lang/Byte");
	final static Type BOOLEAN_TYPE = Type.getObjectType("java/lang/Boolean");
	final static Type SHORT_TYPE = Type.getObjectType("java/lang/Short");
	final static Type CHARACTER_TYPE = Type
			.getObjectType("java/lang/Character");
	final static Type INTEGER_TYPE = Type.getObjectType("java/lang/Integer");
	final static Type FLOAT_TYPE = Type.getObjectType("java/lang/Float");
	final static Type LONG_TYPE = Type.getObjectType("java/lang/Long");
	final static Type DOUBLE_TYPE = Type.getObjectType("java/lang/Double");
	final static Type NUMBER_TYPE = Type.getObjectType("java/lang/Number");
	final static Type OBJECT_TYPE = Type.getObjectType("java/lang/Object");
	final static Method BOOLEAN_VALUE = Method
			.getMethod("boolean booleanValue()");
	final static Method CHAR_VALUE = Method.getMethod("char charValue()");
	public final static Method INT_VALUE = Method.getMethod("int intValue()");
	final static Method FLOAT_VALUE = Method.getMethod("float floatValue()");
	private final static Method LONG_VALUE = Method
			.getMethod("long longValue()");
	private final static Method DOUBLE_VALUE = Method
			.getMethod("double doubleValue()");
	static
	{
		DESCRIPTORS = new HashMap<Class<?>, String>();
		DESCRIPTORS.put(Byte.TYPE, "B");
		DESCRIPTORS.put(Character.TYPE, "C");
		DESCRIPTORS.put(Double.TYPE, "D");
		DESCRIPTORS.put(Float.TYPE, "F");
		DESCRIPTORS.put(Integer.TYPE, "I");
		DESCRIPTORS.put(Long.TYPE, "J");
		DESCRIPTORS.put(Short.TYPE, "S");
		DESCRIPTORS.put(Boolean.TYPE, "Z");
		METHODS = new HashMap<Class<?>, Method>();
		METHODS.put(Byte.TYPE, INT_VALUE);
		METHODS.put(Character.TYPE, CHAR_VALUE);
		METHODS.put(Double.TYPE, DOUBLE_VALUE);
		METHODS.put(Float.TYPE, FLOAT_VALUE);
		METHODS.put(Integer.TYPE, INT_VALUE);
		METHODS.put(Long.TYPE, LONG_VALUE);
		METHODS.put(Short.TYPE, INT_VALUE);
		METHODS.put(Boolean.TYPE, BOOLEAN_VALUE);
		WRAP_TYPES = new HashMap<Class<?>, Type>();
		WRAP_TYPES.put(Byte.TYPE, BYTE_TYPE);
		WRAP_TYPES.put(Character.TYPE, CHARACTER_TYPE);
		WRAP_TYPES.put(Double.TYPE, DOUBLE_TYPE);
		WRAP_TYPES.put(Float.TYPE, FLOAT_TYPE);
		WRAP_TYPES.put(Integer.TYPE, INTEGER_TYPE);
		WRAP_TYPES.put(Long.TYPE, LONG_TYPE);
		WRAP_TYPES.put(Short.TYPE, SHORT_TYPE);
		WRAP_TYPES.put(Boolean.TYPE, BOOLEAN_TYPE);
	}

	public static String getPrimitiveClassDesc(Class<?> c)
	{
		return DESCRIPTORS.get(c);
	}

	public static Method getPrimitiveMethod(Class<?> c)
	{
		return METHODS.get(c);
	}

	public static Type getWrapType(Class<?> c)
	{
		return WRAP_TYPES.get(c);
	}

	public static String getSetterDesc(Class<?> c)
	{
		Class<?> out = c;
		// if (c.isPrimitive()) out = BonesOfBeans.wrapperEquivalentOf(c);
		return Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type
				.getType(out) });
	}

	public static void unbox(MethodVisitor mv, Class<?> c)
	{
		Method m = METHODS.get(c);
		if (m != null)
		{
			mv.visitMethodInsn(INVOKEVIRTUAL, WRAP_TYPES.get(c)
					.getInternalName(), m.getName(), m.getDescriptor());
		}
	}

	public static void box(MethodVisitor mv, Class<?> c)
	{
		Method m = METHODS.get(c);
		if (m != null)
		{
			Type wr = WRAP_TYPES.get(c);
			mv.visitMethodInsn(INVOKESTATIC, WRAP_TYPES.get(c)
					.getInternalName(), "valueOf", Type.getMethodDescriptor(wr,
					new Type[] { Type.getType(c) }));
		}
	}

	public static void main(String[] args)
	{
		// "(I)Ljava/lang/Integer;");
		// box(null, Integer.TYPE);
		System.out.println(getPrimitiveClassDesc(Integer.TYPE));
		if (true) return;
		String s = Type.getMethodDescriptor(Type.getType(Boolean.class),
				new Type[] { Type.getType(Boolean.TYPE) });
		try
		{
			java.lang.reflect.Method m = Boolean.class.getMethod("valueOf",
					new Class[] { Boolean.TYPE });
			System.out.println("RetType: " + Type.getReturnType(m));
			System.out.println("Desc: " + Type.getMethodDescriptor(m));
			// javax.swing.AbstractButton
		}
		catch (Exception ex)
		{
		}
		String s1 = Type.getMethodDescriptor(Type.VOID_TYPE, new Type[] { Type
				.getType(String.class) });
		System.out.println(s);
		System.out.println(getSetterDesc(String.class));
		System.out.println(s1);
		// if(true) return;
		Class<?> cls = JButton.class;
		try
		{
			// temp.javax_swing_JComponent.class;
			// Class gen = ClassGenerator.getClass(cls);
			// if(gen == null)
			// gen = new ClassGenerator().generate(cls);
			// GeneratedClass inst = (GeneratedClass) gen.newInstance();
			// System.out.println("GENERATE: " + gen.getName());
			String path = "E:\\temp\\xxx\\temp\\javax_swing_JMenuBar.class";
			// ClassReader cr = new ClassReader(
			// new FileInputStream(path));
			// ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			// cr.accept(new CheckClassAdapter(cw), ClassReader.SKIP_FRAMES);
			// TraceClassVisitor tv = new TraceClassVisitor(new
			// PrintWriter(System.err));
			// new ClassReader(cw.toByteArray()).accept(tv,
			// ClassReader.SKIP_FRAMES);
			// CheckClassAdapter.main(new String[]{path} );
			ClassReader cr = new ClassReader(new FileInputStream(path));
			CheckClassAdapter.verify(cr, true, new PrintWriter(System.err));
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static Map<String, Class<?>> getCtrSlots(HyperGraph hg, SwingType type)
	{
		ConstructorLink link = (ConstructorLink) hg.get(type.getCtrHandle());
		if (link == null) return null;
		boolean factory = (link instanceof FactoryConstructorLink);
		Map<String, Class<?>> map = new HashMap<String, Class<?>>();
		int nArgs = (factory) ? link.getArity() - 2 : link.getArity();
		for (int i = 0; i < nArgs; i++)
		{
			Class<?> c = link.getTypeAt(hg, i);
			Slot s = link.getSlotAt(hg, i);
			if(s != null)
			   map.put(s.getLabel(), c);
		}
		return map;
	}

	public static String[] getCtrSlotNames(HyperGraph hg, SwingType type)
	{
		ConstructorLink link = (ConstructorLink) hg.get(type.getCtrHandle());
		if (link == null) return null;
		boolean factory = (link instanceof FactoryConstructorLink);
		int nArgs = (factory) ? link.getArity() - 2 : link.getArity();
		String[] names = new String[nArgs];
		for (int i = 0; i < nArgs; i++)
		{
			Slot s = link.getSlotAt(hg, i);
			if(s != null)
			   names[i] = s.getLabel();
			else{
				System.err.println("NULL SLOT at: " + i + ":" + type.getJavaClass());
			}
		}
		return names;
	}

	public static Object getConstructor(HyperGraph hg, SwingType type)
	{
		//System.out.println("getCtr: " + type.getJavaClass() + ":"
		//		+ type.getCtrHandle());
		
		if(type.getCtrHandle() == hg.getHandleFactory().nullHandle())return null;
		ConstructorLink link = (ConstructorLink) hg.get(type.getCtrHandle());
		if (link == null) return null;
		if (link instanceof FactoryConstructorLink) return getFactoryMethod(hg, type);
		Class<?>[] types = new Class[0];
		if (link != null)
		{
			int nArgs = link.getArity();
			types = new Class[nArgs];
			for (int i = 0; i < nArgs; i++)
			{
				Class<?> c = link.getTypeAt(hg, i);
				if(c != null)
				  types[i] = c;
				else
					System.err.println("getCtr - null arg: " + i + ":" + type.getJavaClass());
			}
		}
		Class<?> beanClass = type.getJavaClass();
		Constructor<?> ctr = null;
		try
		{
			ctr = beanClass.getDeclaredConstructor(types);
			ctr.setAccessible(true);
			return ctr;
		}
		catch (Exception e)
		{
			for (int i = 0; i < types.length; i++)
			{
				if (types[i] == null)
					System.err.println("NullParam at index: " + i + ":"
							+ beanClass);
				types[i] = BonesOfBeans.primitiveEquivalentOf(types[i]);
			}
			try
			{
				ctr = beanClass.getDeclaredConstructor(types);
				ctr.setAccessible(true);
				return ctr;
			}
			catch (Exception ex)
			{
				System.err.println("CTR: " + beanClass + ":" + ex.toString());
				for (int i = 0; i < types.length; i++)
				{
					System.err.println("" + types[i]);
				}
				return null;
			}
		}
	}
	
	public static java.lang.reflect.Method getFactoryMethod(HyperGraph hg, SwingType type)
	{
		FactoryConstructorLink link = (FactoryConstructorLink) 
		       hg.get(type.getCtrHandle());
		if (link == null) return null;
		int nArgs = link.getArity() - 2;
		Class<?>[] types  = new Class[nArgs];
		Class<?> c = null;
		String method_name = null;
		try{
		//Class<?> 
		 c = link.getDeclaringClass(hg);
		//String
		 method_name = link.getMethodName(hg);
		for (int i = 0; i < nArgs; i++)
			types[i] = link.getTypeAt(hg, i);
		//try{
			return c.getMethod(method_name, types);
		}catch(Exception ex){
			
			try
			{
				for (int i = 0; i < types.length; i++)
					types[i] = BonesOfBeans.primitiveEquivalentOf(types[i]);
				return c.getMethod(method_name, types);
			}
			catch (Exception ex1)
			{
				throw new HGException("Unable to find factory method: " + method_name +
						" on " + type.getJavaClass().getName() + ". Reason: " + ex);
			}
		}
	}
}
