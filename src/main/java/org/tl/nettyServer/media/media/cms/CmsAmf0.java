package org.tl.nettyServer.media.media.cms;


import org.tl.nettyServer.media.media.mpeg.MpegUtil;
import org.tl.nettyServer.media.util.BufferUtil;

import static org.tl.nettyServer.media.media.mpeg.MpegUtil.ntohs;

public class CmsAmf0 {
   public static byte AMF0_TYPE_NONE =  -1;
    //以下是标准类型
   public static byte AMF0_TYPE_NUMERIC  = 0x00;
   public static byte AMF0_TYPE_BOOLEAN= 0x01;
   public static byte AMF0_TYPE_STRING= 0x02;
   public static byte AMF0_TYPE_OBJECT= 0x03;
   public static byte AMF0_TYPE_MOVIECLIP= 0x04;
   public static byte AMF0_TYPE_NULL= 0x05;
   public static byte AMF0_TYPE_UNDEFINED= 0x06;
   public static byte AMF0_TYPE_REFERENCE= 0x07;
   public static byte AMF0_TYPE_ECMA_ARRAY= 0x08;
   public static byte AMF0_TYPE_OBJECT_END= 0x09;

   public static byte AMF0_TYPE_STRICT_ARRAY= 0x0A;
   public static byte AMF0_TYPE_DATE= 0x0B;
   public static byte AMF0_TYPE_LONG_STRING= 0x0C;
   public static byte AMF0_TYPE_UNSUPPORTED= 0x0D;
   public static byte AMF0_TYPE_RECORD_SET= 0x0E;
   public static byte AMF0_TYPE_XML_OBJECT= 0x0F;
   public static byte AMF0_TYPE_TYPED_OBJECT= 0x10;
   public static byte AMF0_TYPE_AMF3= 0x11;

    void amf0BlockRelease(Amf0Block block)
    {
        if (block != null)
        {
            Amf0Node node = amf0ArrayFirst(block.array_data);
            while (node != null)
            {
                amf0DataFree(amf0ArrayDelete(block.array_data, node));
                node = amf0ArrayFirst(block.array_data);
            }

        }
    }

    byte amf0Block5Value(Amf0Block block, String key, String[] strValue)
    {
        byte type = AMF0_TYPE_NONE;
        if (block != null)
        {
            Amf0Node node = amf0ArrayFirst(block.array_data);
            while (node != null)
            {
                type = amf0Data5Value(node.data, key, strValue);
                if (type != AMF0_TYPE_NONE)
                {
                    break;
                }
                node = node.next;
            }
        }
        return type;
    }
    Amf0Node amf0ArrayFirst(Amf0Array array)
    {
        return array != null ? array.first_element : null;
    }
    byte amf0Data5Value(Amf0Data data, String key, String[] strValue)
    {
        byte type = AMF0_TYPE_NONE;
        if (data != null)
        {
            switch (data.type)
            {
                case 0x03://0x03:
                case 0x08://0x08:
                {
                    Amf0Node nodeKey = amf0ObjectFirst(data);
                    while (nodeKey != null)
                    {
                        if (key.length() == nodeKey.data.string_data.size &&
                                MpegUtil.strncmp(nodeKey.data.string_data.mbstr, key, nodeKey.data.string_data.size) == 0)
                        {
                            Amf0Node nodeValue = nodeKey.next;
                            if (nodeValue.data.type == 0x00)
                            {
                                type = 0x00;
                                strValue[0] = String.valueOf(nodeValue.data.number_data);
                                break;
                            }
                            else if (nodeValue.data.type == 0x01)
                            {
                                type = 0x01;
                                if (nodeValue.data.boolean_data == 0x00)
                                {
                                    strValue[0] = "false";
                                }
                                else
                                {
                                    strValue[0] = "true";
                                }
                                break;
                            }
                            else if (nodeValue.data.type == 0x02)
                            {
                                type = 0x02;
                                strValue[0] = nodeValue.data.string_data.mbstr;
                                break;
                            }
                            else if (nodeValue.data.type == 0x0B)
                            {
                                type = 0x0B;
                                //char szTimeZone[30] = { 0 };
                                //sprintf(szTimeZone, "%llu-%d", nodeValue.data.date_data.milliseconds, nodeValue.data.date_data.timezone);

                                strValue[0] = Long.toUnsignedString(nodeValue.data.date_data.milliseconds) + nodeValue.data.date_data.timezone;
                                break;
                            }
                            else if (nodeValue.data.type ==  0x03 ||
                                    nodeValue.data.type ==  0x08)
                            {
                                type = amf0Data5Value(nodeValue.data, key, strValue);
                                if (type !=  AMF0_TYPE_NONE)
                                {
                                    break;
                                }
                            }
                        }
                        nodeKey = amf0ObjectNext(nodeKey);
                    }
                }
                break;
                default:
                    break;
            }
        }
        return type;
    }
    Amf0Node amf0ObjectFirst(Amf0Data data)
    {
        return data != null ? amf0ArrayFirst(data.array_data) : null;
    }
    Amf0Node amf0ObjectNext(Amf0Node node)
    {
        if (node != null)
        {
            Amf0Node next = node.next;
            if (next != null)
            {
                return next.next;
            }
        }
        return null;
    }
    int amf0TypeParse(byte[] pData,int index, int len, byte[] type)
    {
        if (len < 1)
        {
            type[0] = AMF0_TYPE_NONE;
            return index + len;
        }
        type[0] = pData[index];
        return ++index;
    }
    byte[] bit64Reversal(byte[] bit,int index)
    {
        // 		if (isBigEndian())
        // 		{
        // 			return bit;
        // 		}
        int pBegin = index;
        int pEnd = index + 7;

        for (int i = 0; i < 4; ++i)
        {
            byte temp;
            temp = bit[pBegin];
            bit[pBegin] = bit[pEnd];
            bit[pEnd] = temp;
            ++pBegin;
            --pEnd;
        }
        return bit;
    }
    Amf0Data amf0DataNew(byte type)
    {
        Amf0Data data = new Amf0Data();
        if (data != null)
        {
            data.type = type;
        }
        return data;
    }
    Amf0Data amf0NumberNew(double value)
    {
        Amf0Data data = amf0DataNew((byte) 0x00);
        if (data != null)
        {
            data.number_data = value;
        }
        return data;
    }
    int amf0NumberParse(byte[] pData,int index, int len, Amf0Data data)
    {
        if (len < 8)
        {
			data = null;
            return index += len;
        }
        byte[] bit = bit64Reversal(pData,index);
        double value = bit[index];
		data = amf0NumberNew(value);
        if (data == null)
        {
            assert(true);
        }
        return index + 8;
    }
    Amf0Data amf0ArrayPush(Amf0Array array, Amf0Data data)
    {
        if (array != null && data != null)
        {
            Amf0Node node = new Amf0Node();
            if (node != null) {
                node.data = data;
                node.next = null;
                node.prev = null;
                if (array.size == 0) {
                    array.first_element = node;
                    array.last_element = node;
                }
                else
                {
                    array.last_element.next = node;
                    node.prev = array.last_element;
                    array.last_element = node;
                }
                ++(array.size);
                return data;
            }
        }
        return null;
    }
    Amf0Data amf0BlockPush(Amf0Block block, Amf0Data data)
    {
        if (block != null && data != null)
        {
            return amf0ArrayPush(block.array_data, data);
        }
        return null;
    }
    Amf0Data amf0BooleanNew(byte value)
    {
        Amf0Data data = amf0DataNew((byte) 0x01);
        if (data != null)
        {
            data.boolean_data = value;
        }
        return data;
    }
    int amf0BooleanParse(byte[] pData,int index, int len, Amf0Data data)
    {
        if (len < 1)
        {
			data = null;
            return index + len;
        }
        byte pBoolean = pData[index];
		data = amf0BooleanNew(pBoolean);
        if (data == null)
        {
            assert(true);
        }
        return index + 1;
    }

    /* string functions */
    Amf0Data amf0StringNew(byte[] str, int index, char size)
    {
        Amf0Data data = amf0DataNew((byte) 0x02);
        if (data != null)
        {
            data.string_data.size = size;
            byte[] b = new byte[size];
            MpegUtil.memcpy(b,0,str,index,size);
            data.string_data.mbstr = new String(b);
            data.string_data.mbstr += "\0";
        }
        return data;
    }

    int amf0StringParse(byte[]pData,int index, int len, Amf0Data data)
    {
        //兼容 艾米范
		/*if (pData[0] == 0x09)
		{
			pData++;
			int len = *pData;
			pData++;
			pData += len;
		}*/
        if (len < 2)
        {
			data = null;
            return index + len;
        }
        int size = 0;
        //肯能有问题
        char ln = ntohs(pData,index);
        // sizeof(uint16) 根据平台变换大小
        size = 2 + ln;
        if (len < size)
        {
			data = null;
            return index + len;
        }
		data = amf0StringNew(pData,index +2, ln);
        if (data == null)
        {
            assert(true);
        }
        return index + size;
    }
    Amf0Data amf0ObjectNew()
    {
        Amf0Data data = amf0DataNew((byte) 0x03);
        if (data != null)
        {
            data.array_data.size = 0;
            data.array_data.first_element = null;
            data.array_data.last_element = null;
        }
        return data;
    }
    void amf0DataFree(Amf0Data data)
    {
        Amf0Node node = null;
        if (data != null)
        {
            switch ( (data.type & 0xFF))
            {
                case 0x00://0x00:
                    break;
                case 0x01://0x01:
                    break;
                case 0x02://0x02:
                    if (data.string_data.mbstr != null)
                    {
                        data.string_data.mbstr = null;//xfree(data.string_data.mbstr);
                    }
                    break;
                case 0x03://0x03:
                case 0x08://0x08:
                case 0x0A://0x0A:
                {
                    node = amf0ObjectFirst(data);
                    Amf0Node tmp = null;
                    while (node != null)
                    {
                        amf0DataFree(node.data);
                        tmp = node;
                        node = node.next;
                        //delete tmp;
                    }
                    data.array_data.size = 0;
                }
                break;
                case 0x05://0x05:
                case 0x06://0x06:
                    break;
                case 0x0B://0x0B:
                    break;
                /*case AMF0_TYPE_SIMPLEOBJECT:*/
                case 0x10://0x10:
                case 0x11://0x11:
                case 0x09://0x03_END:
                    break; /* end of composite object */
                default:
                    break;
            }
            //delete data;;
        }
    }
    /* string functions */
    Amf0Data amf0StringNew(String str)
    {
        Amf0Data data = amf0DataNew((byte) 0x02);
        if (data != null)
        {
            data.string_data.size = (char) str.length();
            data.string_data.mbstr = str;

            data.string_data.mbstr += "\0";
        }
        return data;
    }
    Amf0Data amf0String(String str)
    {
        return amf0StringNew(str);
    }
    Amf0Data amf0ArrayDelete(Amf0Array array, Amf0Node node)
    {
        if (array == null)
        {
            return null;
        }
        Amf0Data data = null;
        if (node != null)
        {
            if (node.next != null)
            {
                node.next.prev = node.prev;
            }
            if (node.prev != null)
            {
                node.prev.next = node.next;
            }
            if (node == array.first_element)
            {
                array.first_element = node.next;
            }
            if (node == array.last_element)
            {
                array.last_element = node.prev;
            }
            data = node.data;
           // delete node;
            --(array.size);
        }
        return data;
    }
    Amf0Data amf0ArrayPop(Amf0Array array)
    {
        return amf0ArrayDelete(array, array.last_element);
    }
    Amf0Data amf0ObjectAdd(Amf0Data data, String name, Amf0Data element)
    {
        if (amf0ArrayPush(data.array_data, amf0String(name)) != null)
        {
            if (amf0ArrayPush(data.array_data, element) != null)
            {
                return element;
            }
			else
            {
                amf0ArrayPop(data.array_data);
            }
        }
        return null;
    }
    Amf0Data amf0NullNew()
    {
        Amf0Data data = amf0DataNew((byte) 0x05);
        return data;
    }
    Amf0Data amf0EcmaArrayNew()
    {
        Amf0Data data = amf0DataNew((byte) 0x08);
        if (data != null)
        {
            data.array_data.size = 0;
            data.array_data.first_element = null;
            data.array_data.last_element = null;
        }
        return data;
    }
    Amf0Data amf0ArrayNew()
    {
        Amf0Data data = amf0DataNew((byte) 0x0A);
        if (data != null)
        {
            data.array_data.size = 0;
            data.array_data.first_element = null;
            data.array_data.last_element = null;
            return data;
        }
        return null;
    }
    Amf0Data amf0DateNew(long milliseconds, char timezone)
    {
        Amf0Data data = amf0DataNew((byte) 0x0B);
        if (data != null)
        {
            data.date_data.milliseconds = milliseconds;
            data.date_data.timezone = timezone;
        }
        return data;
    }
    int amf0DateParse(byte[] pData,int index, int len, Amf0Data data)
    {
        if (len < 10)
        {
			data = null;
            return index + len;
        }
        pData = bit64Reversal(pData,index);
        //这块有迷惑他为啥不翻转？
        //long minSeconds = *((double *)pData);
        long minSeconds = MpegUtil.byteToLong(pData,index);
        index += 8;
        char zone = MpegUtil.byteToChar(pData,index);
       // zone = ntohs(zone);
		data = amf0DateNew(minSeconds, zone);
        if (data == null)
        {
            return index + len;
        }
        index += 2;
        return index;
    }
    int amf0StrictArrayParse(byte[] pData,int index, int len, Amf0Data ppArray)
    {
        if (len < 4)
        {
			ppArray = null;
            return index + len;
        }
		ppArray = amf0ArrayNew();
        if (ppArray == null)
        {
			ppArray = null;
            return index + len;
        }
        int pEnd = index + len;
        Amf0Data array = ppArray;
        //*((int *)pData); 强制转换了int指针，则指针0位置为 byte[3]所以用ntohl翻转一下
        int num = BufferUtil.byteArrayToInt(pData, index, 4); //MpegUtil.byteToInt(pData, index);
        //num = MpegUtil.ntohl(num);
        byte[] type = new byte[0];
        index += 4;
        for (int i = 0; i < num && index < pEnd; ++i)
        {
            index = amf0TypeParse(pData,index, pEnd - index, type);
            switch (type[0])
            {
                case 0x00:
                {
                    Amf0Data data = null;
                    index = amf0NumberParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x01:
                {
                    Amf0Data data = null;
                    index = amf0BooleanParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x02:
                {
                    Amf0Data data = null;
                    index = amf0StringParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x03:
                {
                    Amf0Data data = null;
                    index = amf0ObjectParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x04:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle MovieClip[ %d ] +++", 0x04));
                    return pEnd;
                }
                case 0x05:
                {
                    System.err.println(String.format("+++ amf0StrictArray ignore null[ %d ] +++", 0x05));
                    Amf0Data data = amf0NullNew();
                    if (data != null)
                    {
                        amf0ArrayPush(array.array_data, data);
                    }
                }
                break;
                case 0x06:
                {
                    System.err.println(String.format("+++ amf0StrictArray ignore UnDefined[ %d ] +++", 0x06));
                    //++pData;
                }
                break;
                case 0x07:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle Reference[ %d ] +++", 0x07));
                    return pEnd;
                }
                case 0x08:
                {
                    Amf0Data data = null;
                    index = amf0EcmaArrayParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x0A:
                {
                    Amf0Data data = null;
                    index = amf0StrictArrayParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x0B:
                {
                    Amf0Data data = null;
                    index = amf0DateParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        return pEnd;
                    }
                    amf0ArrayPush(array.array_data, data);
                }
                break;
                case 0x0C:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle LongString[ %d ] +++", 0x0C));
                    return pEnd;
                }
                case 0x0D:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle UnSupported[ %d ] +++", 0x0D));
                    return pEnd;
                }
                case 0x0E:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle RecordSet[ %d ] +++", 0x0E));
                    return pEnd;
                }
                case 0x0F:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle XmlObject[ %d ] +++", 0x0F));
                    return pEnd;
                }
                case 0x10:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle TypedObject[ %d ] +++", 0x10));
                    return pEnd;
                }
                case 0x11:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle AMF3[ %d ] +++", 0x11));
                    return pEnd;
                }
                default:
                {
                    System.err.println(String.format("+++ amf0StrictArray UnHandle UnKnow Type[ %d ] +++", type));
                    return pEnd;
                } 
            }
        }
        return index;
    }

    int amf0EcmaArrayParse(byte[] pData,int index, int len, Amf0Data ppEcma)
    {
        if (len < 4)
        {
			ppEcma = null;
            return index + len;
        }
		ppEcma = amf0EcmaArrayNew();
        if (ppEcma == null)
        {
            assert(true);
        }
        int pEnd = index + len;
        Amf0Data ecma = ppEcma;
        boolean bFinish = false;
        byte[] type = new byte[0];
        //跳过 ecma array 长度，该长度有时候会出现0情况
        index += 4;

        do
        {
            //key 一定是string类型
            Amf0Data dataString = null;
            index = amf0StringParse(pData, index,pEnd - index, dataString);
            if (dataString == null)
            {
                return pEnd;
            }
            //value
            index = amf0TypeParse(pData,index, pEnd - index, type);
            switch (type[0])
            {
                case 0x00:
                {
                    Amf0Data data = null;
                    index = amf0NumberParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma, dataString.string_data.mbstr, data);
                }
                break;
                case 0x01:
                {
                    Amf0Data data = null;
                    index = amf0BooleanParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma, dataString.string_data.mbstr, data);
                }
                break;
                case 0x02:
                {
                    Amf0Data data = null;
                    index = amf0StringParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma, dataString.string_data.mbstr, data);
                }
                break;
                case 0x03:
                {
                    Amf0Data data = null;
                    index = amf0ObjectParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma,dataString.string_data.mbstr, data);
                }
                break;
                case 0x04:
                {
                    System.out.println("+++ amf0EcmaArray UnHandle MovieClip[ %d ] +++"+ 0x04);
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x05:
                {
                    System.out.println("+++ amf0EcmaArray ignore null[ %d ] +++"+ 0x05);
                    Amf0Data data = amf0NullNew();
                    if (data != null)
                    {
                        amf0ObjectAdd(ecma,dataString.string_data.mbstr, data);
                    }
                }
                break;
                case 0x06:
                {
                    System.out.println("+++ amf0EcmaArray ignore UnDefined[ %d ] +++"+ 0x06);
                    //++pData;
                }
                break;
                case 0x07:
                {
                    System.out.println("+++ amf0EcmaArray UnHandle Reference[ %d ] +++"+0x07);
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x08:
                {
                    Amf0Data data = null;
                    index = amf0EcmaArrayParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma,dataString.string_data.mbstr, data);
                }
                break;
                case 0x0A:
                {
                    Amf0Data data = null;
                    index = amf0StrictArrayParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma,dataString.string_data.mbstr, data);
                }
                break;
                case 0x0B:
                {
                    Amf0Data data = null;
                    index = amf0DateParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(ecma,dataString.string_data.mbstr, data);
                }
                break;
                case 0x0C:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle LongString[ %d ] +++", 0x0C));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0D:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle UnSupported[ %d ] +++", 0x0D));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0E:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle RecordSet[ %d ] +++", 0x0E));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0F:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle XmlObject[ %d ] +++", 0x0F));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x10:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle TypedObject[ %d ] +++", 0x10));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x11:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle AMF3[ %d ] +++", 0x11));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                default:
                {
                    System.out.println(String.format("+++ amf0EcmaArray UnHandle UnKnow Type[ %d ] +++", type));
                    amf0DataFree(dataString);
                    return pEnd;
                }
            }
            if (pData[0] == 0x00 && pData[1] == 0x00 && pData[2] == 0x09)
            {
                //object 结束
                index += 3;
                bFinish = true;
            }
            amf0DataFree(dataString);
        } while (!bFinish && index < pEnd);
        return index;
    }
    int amf0ObjectParse(byte[]  pData,int index, int len, Amf0Data ppObject)
    {
		ppObject = amf0ObjectNew();
        if (ppObject == null)
        {
			ppObject = null;
            return index + len;
        }
        Amf0Data object = ppObject;
        boolean bFinish = false;
        byte[] type = new byte[0];
        int pEnd = index + len;
        do
        {
            //key 一定是string类型
            Amf0Data dataString = null;
            index = amf0StringParse(pData,index, pEnd - index, dataString);
            if (dataString == null)
            {
                return pEnd;
                //assert(true);
            }
            //value
            index = amf0TypeParse(pData, index,pEnd - index, type);
            switch ( (type[0]  & 0xFF))
            {
                case 0x00:
                {
                    Amf0Data data = null;
                    index = amf0NumberParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                break;
                case 0x01:
                {
                    Amf0Data data = null;
                    index = amf0BooleanParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                break;
                case 0x02:
                {
                    Amf0Data data = null;
                    index = amf0StringParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                break;
                case 0x03:
                {
                    Amf0Data data = null;
                    index = amf0ObjectParse(pData, index,pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                break;
                case 0x04:
                {
                    System.out.println("+++ amf0ObjectParse UnHandle MovieClip[ %d ] +++"+ 0x04);
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x05:
                {
                    System.out.println("+++ amf0ObjectParse ignore null[ %d ] +++"+ 0x05);
                    Amf0Data data = amf0NullNew();
                    if (data != null)
                    {
                        amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                    }
                }
                break;
                case 0x06:
                {
                    System.out.println("+++ amf0ObjectParse ignore UnDefined[ %d ] +++"+ 0x06);
                    //++pData;
                }
                break;
                case 0x07:
                {
                    System.out.println("+++ amf0ObjectParse UnHandle Reference[ %d ] +++"+ 0x07);
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x08:
                {
                    Amf0Data data = null;
                    index = amf0EcmaArrayParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                case 0x0A:
                {
                    Amf0Data data = null;
                    index = amf0StrictArrayParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                case 0x0B:
                {
                    Amf0Data data = null;
                    index = amf0DateParse(pData,index, pEnd - index, data);
                    if (data == null)
                    {
                        amf0DataFree(dataString);
                        return pEnd;
                    }
                    amf0ObjectAdd(object,dataString.string_data.mbstr, data);
                }
                break;
                case 0x0C:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle LongString[ %d ] +++", 0x0C));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0D:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle UnSupported[ %d ] +++", 0x0D));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0E:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle RecordSet[ %d ] +++", 0x0E));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x0F:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle XmlObject[ %d ] +++", 0x0F));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x10:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle TypedObject[ %d ] +++", 0x10));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                case 0x11:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle AMF3[ %d ] +++", 0x11));
                    amf0DataFree(dataString);
                    return pEnd;
                }
                default:
                {
                    System.out.println(String.format("+++ amf0ObjectParse UnHandle UnKnow Type[ %d ] +++", type));
                    amf0DataFree(dataString);
                    return pEnd;
                }
            }
            if ( (pData[0] & 0xFF) == 0x00 && (pData[1] & 0xFF) == 0x00 && (pData[2] & 0xFF) == 0x09)
            {
                //object 结束
                index += 3;
                bFinish = true;
            }
            amf0DataFree(dataString);
        } while (!bFinish && index < pEnd);
        return index;
    }

    Amf0Block amf0Parse(byte[] buf, int index, int len)
    {
        if (buf == null || len <= 0)
        {
            return null;
        }
        Amf0Block afm0Block = amf0BlockNew();
        byte[] pData = buf;
        int pEnd = len ;// buf + len;
        byte[] type = {0};
        boolean bGetCmd = false;

        boolean gotoLabe = true;
        END:

        while (gotoLabe && index < pEnd)
        {
            index = amf0TypeParse(pData,index, pEnd - index, type);
            switch (type[0])
            {
                case 0x00:
                {
                    Amf0Data data = null;
                    index = amf0NumberParse(pData, index,pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
					    break END;
                    }
                }
                break;
                case 0x01:
                {
                    Amf0Data data = null;
                    index = amf0BooleanParse(pData,index, pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x02:
                {
                    Amf0Data data = null;
                    index = amf0StringParse(pData,index, pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                        if (bGetCmd == false)
                        {
                            afm0Block.cmd = data.string_data.mbstr;
                            //std::transform(afm0Block.cmd.begin(), afm0Block.cmd.end(), afm0Block.cmd.begin(), tolower);
                        }
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x03:
                {
                    Amf0Data data = null;
                    index = amf0ObjectParse(pData,index, pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x04:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle MovieClip[ %d ] +++", 0x04));
                    gotoLabe = false;
                    break END;
                }
                case 0x05:
                {
                    Amf0Data data = amf0NullNew();
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x06:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle UnDefined[ %d ] +++", 0x06));
                    //++pData;
                }
                break;
                case 0x07:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle Reference[ %d ] +++", 0x07));
                    gotoLabe = false;
                    break END;
                }
                case 0x08:
                {
                    Amf0Data data = null;
                    index = amf0EcmaArrayParse(pData, index,pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x09:
                {
                    gotoLabe = false;
                    break END;
                }
                case 0x0A:
                {
                    Amf0Data data = null;
                    index = amf0StrictArrayParse(pData,index, pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x0B:
                {
                    Amf0Data data = null;
                    index = amf0DateParse(pData, index,pEnd - index, data);
                    if (data != null)
                    {
                        amf0BlockPush(afm0Block, data);
                    }
                    else
                    {
                        gotoLabe = false;
                        break END;
                    }
                }
                break;
                case 0x0C:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle LongString[ %d ] +++", 0x0C));
                    gotoLabe = false;
                    break END;
                }
                case 0x0D:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle UnSupported[ %d ] +++", 0x0D));
                    gotoLabe = false;
                    break END;
                }
                case 0x0E:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle RecordSet[ %d ] +++", 0x0E));
				 gotoLabe = false;
					    break END;
                }
                case 0x0F:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle XmlObject[ %d ] +++", 0x0F));
				 gotoLabe = false;
					    break END;
                }
                case 0x10:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle TypedObject[ %d ] +++", 0x10));
				 gotoLabe = false;
					    break END;
                }
                case 0x11:
                {
                    System.out.println(String.format("+++ amf0Parse UnHandle AMF3[ %d ] +++", 0x11));
				 gotoLabe = false;
					    break END;
                }
                default:
                {
                    gotoLabe = false;
                  break END;
                }
            }
            bGetCmd = true;
        }
        return afm0Block;
    }

    Amf0Block amf0BlockNew()
    {
        Amf0Block block = new Amf0Block();
        if (block != null)
        {
            block.array_data.size = 0;
            block.array_data.first_element = null;
            block.array_data.last_element = null;
        }
        return block;
    }
}
