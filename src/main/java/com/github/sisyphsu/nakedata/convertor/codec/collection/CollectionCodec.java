package com.github.sisyphsu.nakedata.convertor.codec.collection;

import com.github.sisyphsu.nakedata.convertor.codec.Codec;
import com.github.sisyphsu.nakedata.convertor.reflect.XType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collection's codec
 *
 * @author sulin
 * @since 2019-05-13 18:40:23
 */
public class CollectionCodec extends Codec {

    /**
     * Convert Any Collection to Collection, support GenericType convert.
     *
     * @param src  Collection
     * @param type Type
     * @return Collection
     */
    public Collection toCollection(Collection src, XType type) {
        if (src == null)
            return null;
        if (checkCompatible(src, type))
            return src;
        // use ArrayList as default Collection
        XType<?> genericType = type.getParameterizedType();
        List list = new ArrayList();
        for (Object o : src) {
            list.add(convert(o, genericType));
        }
        return list;
    }

    /**
     * Check whether src is compatible with tgtType
     */
    protected boolean checkCompatible(Collection src, XType tgtType) {
        if (src == null)
            return true; // null compatible with everything
        // TODO check class, empty special
        // TODO check generic type, and object type compare
        return false;
    }

}
