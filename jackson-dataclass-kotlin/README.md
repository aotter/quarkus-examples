# Jackson JSON + Kotlin Data Class 好難用？

其實 Jackson 有額外製作 Kotlin 用的額外套件可以改善 deserialization 成 Kotlin data class 的時候會遇到的問題！

https://github.com/FasterXML/jackson-module-kotlin

Quarkus 在使用 `resteasy-reactive` 時，再加入 `jackson-module-kotlin` ：

```
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-resteasy-reactive-jackson</artifactId>
</dependency>
<dependency>
  <groupId>com.fasterxml.jackson.module</groupId>
  <artifactId>jackson-module-kotlin</artifactId>
  <version>2.13.3</version>
</dependency>
```

Quarkus 就會自動使用 `jackson-module-kotlin` 所特製的 `ObjectMapper` ！

參考資料：
https://quarkus.io/guides/kotlin#kotlin-jackson

## jackson-module-kotlin 特性

### 可直接支援 Kotlin Data Class 不需要 no-arg

若沒有 `jackson-module-kotlin` ，當 Jackson 要將 JSON String deserialize 成任何 data class 時，都會因為 data class 沒有無參數的 constructor ，而跳出 `com.fasterxml.jackson.databind.exc.InvalidDefinitionException` 。

有做法是使用 Kotlin 提供的 no-arg plugin 讓 data class 產生出無參數的 constructor ，就能讓 Jackson 順利綁定資料。

但有了 `jackson-module-kotlin` 就不需要以上的設定了！

### 支援 default value

Data class 上的 default value 會自動指定給 request 沒有設定的屬性。

### 沒有缺點了？

還是有，以下就介紹需要額外設定的選項，讓 deserialization 更直覺！分別是：

1. **Enable `DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES`**
   這可以讓理論上不該是 primitive type 的欄位被當成 primitive type 然後塞進預設值
2. **Disable `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES`**
   這可以讓 Jackson 自動忽略沒有寫在 data class 中的屬性，而不要丟 `com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException`

## 差別比較

以上 `MainResource` 分別使用了 4 種不同參數的 ObjectMapper 來做比較：

1. 使用預設的 `ObjectMapper` 不做調整
2. 使用 `jackson-module-kotlin` 的 `jacksonObjectMapper` 但不做調整
3. `jacksonObjectMapper` + enable `FAIL_ON_NULL_FOR_PRIMITIVES`
4. `jacksonObjectMapper` + enable `FAIL_ON_NULL_FOR_PRIMITIVES` + disable `FAIL_ON_UNKNOWN_PROPERTIES`

Data Class 程式碼：
```kotlin
data class PageRequest(
    val search: String?,
    val page: Int,
    val show: Int = 20,
    val beforeTimestamp: Long?
)
```

以下為 四種方式的 Input + Output 比對表格：

| Input JSON | `ObjectMapper` | `jacksonObjectMapper` | `jacksonObjectMapper` + primitive | `jacksonObjectMapper` 大全餐 |
|---|---|---|---|---|
| <pre>{}</pre> | 💥<br>`InvalidDefinitionException` | ✔<br><pre>{<br>  "search": null,<br>  "page": 0,<br>  "show": 20,<br>  "beforeTimestamp": null <br>}</pre> | 💥<br>`MismatchedInputException` | 💥<br>`MismatchedInputException` |
| <pre>null</pre> | 💥<br>`MismatchedInputException` | 💥<br>`MismatchedInputException` | 💥<br>`MismatchedInputException` | 💥<br>`MismatchedInputException` |
| <pre>{<br>  "search": "text",<br>  "page": 2,<br>  "show": 5<br>}</pre> | 💥<br>`InvalidDefinitionException` | ✔<br><pre>{<br>  "search": "text",<br>  "page": 2,<br>  "show": 5,<br>  "beforeTimestamp": null <br>}</pre> | ✔<br><pre>{<br>  "search": "text",<br>  "page": 2,<br>  "show": 5,<br>  "beforeTimestamp": null <br>}</pre> | ✔<br><pre>{<br>  "search": "text",<br>  "page": 2,<br>  "show": 5,<br>  "beforeTimestamp": null <br>}</pre> |
| <pre>{ "hello": "world" }</pre> | 💥<br>`InvalidDefinitionException` | 💥<br>`UnrecognizedPropertyException` | 💥<br>`MismatchedInputException` | 💥<br>`MismatchedInputException` |
| <pre>{<br>  "hello": "world",<br>  "page": 1<br>}</pre> | 💥<br>`InvalidDefinitionException` | 💥<br>`UnrecognizedPropertyException` | 💥<br>`UnrecognizedPropertyException` | ✔<br><pre>{<br>  "search": null,<br>  "page": 1,<br>  "show": 20,<br>  "beforeTimestamp": null <br>}</pre> |

參考資料：
https://github.com/FasterXML/jackson-module-kotlin/issues/130
https://github.com/FasterXML/jackson-module-kotlin/issues/130#issuecomment-376688125