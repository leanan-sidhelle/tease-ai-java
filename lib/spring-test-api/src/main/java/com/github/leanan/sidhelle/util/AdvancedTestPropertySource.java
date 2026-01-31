/*
 * BECAUSE SPRING SUCKS AND CANNOT ENVISION METHOD LEVEL SPECIFICATION!!!!
 */
package com.github.leanan.sidhelle.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.core.io.support.PropertySourceFactory;

/**
 * TODO:
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
//TODO: what to do about this? @Repeatable(TestPropertySources.class)
public @interface AdvancedTestPropertySource {

	/**
	 * Alias for {@link #locations}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #locations}, but it may be used <em>instead</em> of {@link #locations}.
	 * @see #locations
	 */
	@AliasFor("locations")
	String[] value() default {};

	/**
	 * The resource locations of properties files to be loaded into the
	 * {@code Environment}'s set of {@code PropertySources}.
	 * <p>Each location will be added to the enclosing {@code Environment} as its
	 * own property source, in the order declared (or in the order in which resource
	 * locations are resolved when location wildcards are used).
	 * <h4>Supported File Formats</h4>
	 * <p>By default, both traditional and XML-based properties file formats are
	 * supported &mdash; for example, {@code "classpath:/com/example/test.properties"}
	 * or {@code "file:/path/to/file.xml"}. To support a different file format,
	 * configure an appropriate {@link #factory() PropertySourceFactory}.
	 * <h4>Path Resource Semantics</h4>
	 * <p>Each path will be interpreted as a Spring
	 * {@link org.springframework.core.io.Resource Resource}. A plain path
	 * &mdash; for example, {@code "test.properties"} &mdash; will be treated as a
	 * classpath resource that is <em>relative</em> to the package in which the
	 * test class is defined. A path starting with a slash will be treated as an
	 * <em>absolute</em> classpath resource, for example:
	 * {@code "/org/example/test.xml"}. A path which references a
	 * URL (for example, a path prefixed with
	 * {@link org.springframework.util.ResourceUtils#CLASSPATH_URL_PREFIX classpath:},
	 * {@link org.springframework.util.ResourceUtils#FILE_URL_PREFIX file:},
	 * {@code http:}, etc.) will be loaded using the specified resource protocol.
	 * <p>Property placeholders in paths (i.e., <code>${...}</code>) will be
	 * {@linkplain org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 * resolved} against the {@code Environment}.
	 * <p>As of Spring Framework 6.1, resource location patterns are also
	 * supported &mdash; for example, {@code "classpath*:/config/*.properties"}.
	 * <p><strong>WARNING</strong>: a pattern such as {@code "classpath*:/config/*.properties"}
	 * may be effectively equivalent to an explicit enumeration of resource locations such as
	 * <code>{"classpath:/config/mail.properties", classpath:/config/order.properties"}</code>;
	 * however, the two declarations will result in different keys for the context
	 * cache since the pattern cannot be eagerly resolved to concrete locations.
	 * Consequently, to benefit from the context cache you must ensure that you
	 * consistently use either patterns or explicit enumerations of resource
	 * locations within your test suite.
	 * <h4>Default Properties File Detection</h4>
	 * <p>See the class-level Javadoc for a discussion on detection of defaults.
	 * <h4>Precedence</h4>
	 * <p>Properties loaded from resource locations have lower precedence than
	 * inlined {@link #properties}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used <em>instead</em> of {@link #value}.
	 * @see #inheritLocations
	 * @see #value
	 * @see #properties
	 * @see #encoding
	 * @see #factory
	 * @see org.springframework.core.env.PropertySource
	 */
	@AliasFor("value")
	String[] locations() default {};

	/**
	 * Whether test property source {@link #locations} from superclasses
	 * and enclosing classes should be <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that a test class will
	 * <em>inherit</em> property source locations defined by a superclass or
	 * enclosing class. Specifically, the property source locations for a test
	 * class will be appended to the list of property source locations defined
	 * by a superclass or enclosing class. Thus, subclasses and nested classes
	 * have the option of <em>extending</em> the list of test property source
	 * locations.
	 * <p>If {@code inheritLocations} is set to {@code false}, the property
	 * source locations for the test class will <em>shadow</em> and effectively
	 * replace any property source locations defined by a superclass or
	 * enclosing class.
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the {@code "base.properties"}
	 * file as a test property source. In contrast, the {@code ApplicationContext}
	 * for {@code ExtendedTest} will be loaded using the {@code "base.properties"}
	 * <strong>and</strong> {@code "extended.properties"} files as test property
	 * source locations.
	 * <pre class="code">
	 * &#064;TestPropertySource("base.properties")
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 *
	 * &#064;TestPropertySource("extended.properties")
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }</pre>
	 * <p>If {@code @TestPropertySource} is used as a <em>{@linkplain Repeatable
	 * repeatable}</em> annotation, the following special rules apply.
	 * <ol>
	 * <li>All {@code @TestPropertySource} annotations at a given level in the
	 * test class hierarchy (i.e., directly present or meta-present on a test
	 * class) are considered to be <em>local</em> annotations, in contrast to
	 * {@code @TestPropertySource} annotations that are inherited from a
	 * superclass.</li>
	 * <li>All local {@code @TestPropertySource} annotations must declare the
	 * same value for the {@code inheritLocations} flag.</li>
	 * <li>The {@code inheritLocations} flag is not taken into account between
	 * local {@code @TestPropertySource} annotations. Specifically, the property
	 * source locations for one local annotation will be appended to the list of
	 * property source locations defined by previous local annotations. This
	 * allows a local annotation to extend the list of test property source
	 * locations, potentially overriding individual properties.</li>
	 * </ol>
	 * @see #locations
	 */
	boolean inheritLocations() default true;

	/**
	 * <em>Inlined properties</em> in the form of <em>key-value</em> pairs that
	 * should be added to the Spring
	 * {@link org.springframework.core.env.Environment Environment} before the
	 * {@code ApplicationContext} is loaded for the test. All key-value pairs
	 * will be added to the enclosing {@code Environment} as a single test
	 * {@code PropertySource} with the highest precedence. As of Spring Framework
	 * 6.1, multiple key-value pairs may be specified via a single <em>text block</em>.
	 * <h4>Supported Syntax</h4>
	 * <p>The supported syntax for key-value pairs is the same as the
	 * syntax defined for entries in a Java
	 * {@linkplain java.util.Properties#load(java.io.Reader) properties file}:
	 * <ul>
	 * <li>{@code "key=value"}</li>
	 * <li>{@code "key:value"}</li>
	 * <li>{@code "key value"}</li>
	 * </ul>
	 * <p><strong>WARNING</strong>: although properties can be defined using any
	 * of the above syntax variants and any number of spaces between the key and
	 * the value, it is recommended that you use one syntax variant and consistent
	 * spacing within your test suite &mdash; for example, consider always using
	 * {@code "key = value"} instead of {@code "key= value"}, {@code "key=value"},
	 * etc. Similarly, if you define inlined properties using <em>text blocks</em>
	 * you should consistently use text blocks for inlined properties throughout
	 * your test suite. The reason is that the exact strings you provide will be
	 * used to determine the key for the context cache. Consequently, to benefit
	 * from the context cache you must ensure that you define inlined properties
	 * consistently.
	 * <h4>Examples</h4>
	 * <pre class="code">
	 * &#47;&#47; Using an array of strings
	 * &#064;TestPropertySource(properties = {
	 *     "key1 = value1",
	 *     "key2 = value2"
	 * })
	 * &#064;ContextConfiguration
	 * class MyTests {
	 *   // ...
	 * }</pre>
	 * <pre class="code">
	 * &#47;&#47; Using a single text block
	 * &#064;TestPropertySource(properties = """
	 *     key1 = value1
	 *     key2 = value2
	 *     """
	 * )
	 * &#064;ContextConfiguration
	 * class MyTests {
	 *   // ...
	 * }</pre>
	 * <h4>Precedence</h4>
	 * <p>Properties declared via this attribute have higher precedence than
	 * properties loaded from resource {@link #locations}.
	 * <p>This attribute may be used in conjunction with {@link #value}
	 * <em>or</em> {@link #locations}.
	 * @see #inheritProperties
	 * @see #locations
	 * @see org.springframework.core.env.PropertySource
	 */
	String[] properties() default {};

	/**
	 * Whether inlined test {@link #properties} from superclasses and
	 * enclosing classes should be <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that a test class will
	 * <em>inherit</em> inlined properties defined by a superclass or enclosing
	 * class. Specifically, the inlined properties for a test class will be
	 * appended to the list of inlined properties defined by a superclass or
	 * enclosing class. Thus, subclasses and nested classes have the option of
	 * <em>extending</em> the list of inlined test properties.
	 * <p>If {@code inheritProperties} is set to {@code false}, the inlined
	 * properties for the test class will <em>shadow</em> and effectively
	 * replace any inlined properties defined by a superclass or enclosing class.
	 * <p>In the following example, the {@code ApplicationContext} for
	 * {@code BaseTest} will be loaded using only the inlined {@code key1}
	 * property. In contrast, the {@code ApplicationContext} for
	 * {@code ExtendedTest} will be loaded using the inlined {@code key1}
	 * <strong>and</strong> {@code key2} properties.
	 * <pre class="code">
	 * &#064;TestPropertySource(properties = "key1 = value1")
	 * &#064;ContextConfiguration
	 * public class BaseTest {
	 *   // ...
	 * }
	 * &#064;TestPropertySource(properties = "key2 = value2")
	 * &#064;ContextConfiguration
	 * public class ExtendedTest extends BaseTest {
	 *   // ...
	 * }</pre>
	 * <p>If {@code @TestPropertySource} is used as a <em>{@linkplain Repeatable
	 * repeatable}</em> annotation, the following special rules apply.
	 * <ol>
	 * <li>All {@code @TestPropertySource} annotations at a given level in the
	 * test class hierarchy (i.e., directly present or meta-present on a test
	 * class) are considered to be <em>local</em> annotations, in contrast to
	 * {@code @TestPropertySource} annotations that are inherited from a
	 * superclass or enclosing class.</li>
	 * <li>All local {@code @TestPropertySource} annotations must declare the
	 * same value for the {@code inheritProperties} flag.</li>
	 * <li>The {@code inheritProperties} flag is not taken into account between
	 * local {@code @TestPropertySource} annotations. Specifically, inlined
	 * properties for one local annotation will be appended to the list of
	 * inlined properties defined by previous local annotations. This allows a
	 * local annotation to extend the list of inlined properties, potentially
	 * overriding individual properties.</li>
	 * </ol>
	 * @see #properties
	 */
	boolean inheritProperties() default true;

	/**
	 * Specify the character encoding for the given {@linkplain #locations resources}
	 * &mdash; for example, "UTF-8".
	 * <p>If not specified, the default character encoding of the JVM will be used.
	 * @since 6.1
	 */
	String encoding() default "";

	/**
	 * Specify a custom {@link PropertySourceFactory}, if any.
	 * <p>By default, a factory for standard resource files will be used which
	 * supports {@code *.properties} and {@code *.xml} file formats for
	 * {@link java.util.Properties}.
	 * @since 6.1
	 * @see org.springframework.core.io.support.DefaultPropertySourceFactory
	 * @see org.springframework.core.io.support.ResourcePropertySource
	 */
	Class<? extends PropertySourceFactory> factory() default PropertySourceFactory.class;

}