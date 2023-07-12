## 枚举处理
    com.insnail.data.search.biz.handler.ChatHistoryMsgTypeHandler
## json 处理
    mybatis-plus
        https://blog.csdn.net/Lauuii_/article/details/119410646
    
## 控制展示，脱敏，加密

## 一个小坑
    T selectById(Serializable id);

    @TableField(typeHandler = FastjsonTypeHandler.class)
    private List<OssFileBo> ossFiles; 

    处理方式：
    <resultMap id="BaseResultMap" type="com.insnail.consultation.persistence.model.TCustomerProactiveConsultation">
        ...
		<result column="consultation_category" property="consultationCategory" typeHandler="com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler"/>
        ...
    </resultMap>


    resultMap

    https://zhuanlan.zhihu.com/p/79153088
    https://blog.csdn.net/jiamaRay/article/details/108688542

### 审计、log、插件
https://zhuanlan.zhihu.com/p/370464265

### jpa mapper
https://blog.csdn.net/adu003/article/details/105225702
 