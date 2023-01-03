package org.tl.nettyServer.media.buf;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 表示内存泄露已检查过
 *
 * @author TL
 * @date 2023/01/03
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface LeakChecked {
}
