## 新时代来了

chatgpt出来有很长一段时间了，内部推广使用了下，确实是很香。作为开发者的我，在工作中开始有了一些新的习惯，拥抱新时代，chatgpt确实是个不错的工具。

### 一、面向chatGpt编程

以前碰到新的技术工具，一般都是百度或者google一下，即所谓的**面向搜索引擎**编程。但是搜索引擎的结果是参差不齐的，需要阅读不少的文章，才能找到答案，费时费力。如今，我会直接问chatgpt，目前大多数的情景，都可以得到一个不错的答案，chatgpt会给出它的解释和具体的解决方案，并且这个方案是可以直接使用的，迁移成本很低。我简单举几个例子：

#### (1)elasticSearch 完全匹配

需求背景：有个日志数据在elasticSearch搜索引擎里面，其中有个字段tid是一个上下文的唯一id，需要从索引库里面查询精确匹配该字段的数据。

##### 提问：

```java
elastic must match in java
```

##### chatgpt的回答:

```java
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.unit.Fuzziness;
import java.io.IOException;

public class ElasticMustMatch {
    
    private final RestHighLevelClient client; // 你的 Elasticsearch 高级客户端
    
    public ElasticMustMatch(RestHighLevelClient client) {
        this.client = client;
    }
    
    public SearchHits search(String index, String fieldName, String value) throws IOException {
        TermQueryBuilder termQuery = QueryBuilders.termQuery(fieldName, value);
        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery(fieldName, value).fuzziness(Fuzziness.AUTO);
        
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
            .must(termQuery)
            .should(matchQuery);
        
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(boolQuery);
        sourceBuilder.size(10); // 返回的结果数
        
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.source(sourceBuilder);
        
        SearchResponse searchResponse = client.search(searchRequest);
        return searchResponse.getHits();
    }
}
```

##### 迁移使用：

我把chatgpt提供的这个代码片段拷贝到代码，迁移改动很小

```java
public SearchHits query(EsLogReq req) {
    SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
    sourceBuilder.query(QueryBuilders.matchQuery(TRACE_ID, PREFIX + req.getTraceId()).operator(Operator.AND));
    
    SearchRequest searchRequest = new SearchRequest(req.getIndex());
    searchRequest.source(sourceBuilder);
    
    SearchResponse searchResponse = client.search(searchRequest);
    return searchResponse.getHits();
}
```

#### (2)学习和使用新的语言

> 需求背景：想用golang写一个文本处理工具，读取一个csv文件，每一行数据有一个userId，处理每一行后每一行转成一个链接，生成另外一个csv文件。下面是过程中的2次提问：

```go
// 提问一，怎么读取csv文件：
golang read csv
```

```go
// 回答一(这里忽略了解释部分)：
package main

import (
    "encoding/csv"
    "fmt"
    "os"
)

func main() {
    file, err := os.Open("data.csv")
    if err != nil {
        fmt.Println("Error:", err)
        return
    }
    defer file.Close()

    reader := csv.NewReader(file)

    records, err := reader.ReadAll()
    if err != nil {
        fmt.Println("Error:", err)
        return
    }

    for _, record := range records {
        fmt.Println(record)
    }
}
```

在有上下文的情况下，chatgpt会自然的关联起来，问题2不再提及golang:

```go
// 提问二，怎么写文件：
write file
```

```go
// 回答二(这里忽略了解释部分)：
package main

import (
    "fmt"
    "io/ioutil"
)

func main() {
    data := []byte("Hello, World!")
    err := ioutil.WriteFile("output.txt", data, 0644)
    if err != nil {
        fmt.Println(err)
        return
    }
    fmt.Println("Data written to file")
}
```

可以看到，chatgpt给出的demo是比较精准的，简单改下就可以迁移到脚本工具里面。

##### (3)json生成类

对接一些第三方接口时候，会有根据报文生成类的场景，让chatGpt来写：

```java
假设你是一个高级java开发，结合Lombok给下面这个json生成java bean: 
{
    "transactionId": "20230524172209",
    "requestTime": "20230524172249",
    "hcDetail": {
        "hcType": "2",
        "hcid": "7270735",
        "qId": "13577"
    }
}
```

chatgpt的回答：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1eeca9cdd19344fe8749377682d407ec~tplv-k3u1fbpfcp-watermark.image?)

比较好玩的是，如果不提示Lombok，生成的结果是有getter和setter的。chatgpt的理解能力、创造力，让我感到震撼。

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/47524bc24aab4a138fcbec49cffe3dc2~tplv-k3u1fbpfcp-watermark.image?)

## 二、单元测试

日常开发中会写一些测试，但是覆盖的样例和代码不多，能否用chatgpt生成单元测试，提高代码覆盖率。试了下是可以的，效果还不错。比如我有一个java接口：

```java
public interface ExpressionHandler {
    /**
     * 模板渲染
     *
     * @param template
     * @param param
     * @return
     */
    String execScript(String template, Map<String, Object> param);
}
```

> 给chatgpt指令：

```java
    帮我生成单元测试 public interface ExpressionHandler {
    /**
     * 模板渲染
     *
     * @param template
     * @param param
     * @return
     */
    String execScript(String template, Map<String, Object> param);
}
```

> 结果它生成了一个还不错的类，并给出了每个case的解释。：

```java
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ExpressionHandlerTest {
    
    private final ExpressionHandler handler = new YourExpressionHandler(); // 替换成你的实现类
    
    @Test
    public void testExecScriptWithEmptyTemplate() {
        String template = "";
        Map<String, Object> params = new HashMap<>();
        String result = handler.execScript(template, params);
        
        assertEquals("", result);
    }
    
    @Test
    public void testExecScriptWithNullParameters() {
        String template = "Hello, ${name}!";
        Map<String, Object> params = null;
        String result = handler.execScript(template, params);
        
        assertEquals("Hello, ${name}!", result);
    }
    
    @Test
    public void testExecScriptWithValidInput() {
        String template = "Hello, ${name}! Today is ${day}.";
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        params.put("day", "Monday");
        String result = handler.execScript(template, params);
        
        assertEquals("Hello, Alice! Today is Monday.", result);
    }
}
```

```
这里假设你有一个名为 `YourExpressionHandler` 的实现类来处理模板渲染功能。在测试中，我们覆盖了三种情况：
空字符串模板：验证输出是否为空字符串。
空参数映射：验证是否能够正确地使用占位符，而没有使用任何参数。
有效输入：验证是否可以正确地渲染带有变量的模板，并使用给定的参数映射替换变量。
```
> 上面的样例，基本可以直接拿来使用，或者微调下再使用，在帮助改进代码质量方面，效果不错。

## 三、处理线上问题

以下是一次日常报错的处理过程，

### 1.线上应用报错

> 我们有个知识中心，是保险知识库+搜索引擎，给业务销售查询专业问题的。出问题的是搜索接口：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/5f30dd52d99948339263631b1bc6d640~tplv-k3u1fbpfcp-watermark.image?)

### 2.找到错误日志

> 顺着接口找到服务异常日志：

```js
elasticsearch.exceptions.RequestError: RequestError(400, 'search_phase_execution_exception', 
'The length of [content] field of [auto_hebao_onsell_4] doc of [knowledge] index has exceeded
[1000000] - maximum allowed to be analyzed for highlighting. This maximum can be set by
changing the [index.highlight.max_analyzed_offset] index level setting. For large texts,
indexing with offsets or term vectors is recommended!')
```

### 3.寻找解决方案

> 从日志能看出来是和highlight有关系的，但是，不管三七二十一，直接丢给chatGpt：

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/edbc280b516c46a18c0b9535611e57c0~tplv-k3u1fbpfcp-watermark.image?)

```js
PUT /knowledge/_settings
{
    "index.highlight.max_analyzed_offset": 2000000
}
```

> 可以看到,gpt给出了解释，并给出了解决方案。让运维小伙伴按照这个设置操作，业务那边就正常了。只能说chatgpt 牛逼！

## 四、更大的世界

以上案例都是把chatgpt作为辅助工具的，大部分场景还是依赖人力。往更大的世界去思考：

### 蒋炎岩老师：智能编译，编译过程中算法替换

> 之前在老师的b站视频课程，蒋老师给了一个想法：编译器目前做的是指令级别的优化，有没有可能做到把“冒泡”换成“快排”这种层级的优化？

### gpt能写完整代码吗

> 上面用golang写工具的例子，有没有可能chatgpt根据需求完成代码编写呢？最近看的一个有意思的文章：

*   [我用低代码结合ChatGPT开发，每天多出1小时摸鱼](https://juejin.cn/post/7239593701806653495)

### chatgpt与人

> 人在驾驭机器还是机器在驾驭我们?在试用AiXcoder的时候，有时候会觉得它在教我做事。以前的提示工具更多是一种辅助工具，在ai时代，工具提供的代码可以仿佛是它在写代码，而我是工具。

## 总结

> 在chatgpt时代下，开发的日常流程都发生了变化，开发阶段、测试阶段、维护阶段都可以借助chatgpt做出改进。
> 拥抱新时代吧！
