package com.eypa.app.api;

import com.eypa.app.model.Category;
import com.eypa.app.model.Tag;
import com.eypa.app.model.Term;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

/**
 * 自定义 GSON Deserializer，用于处理  API 返回的 Term 对象.
 * 它会检查 "taxonomy" 字段，并根据其值 ("category" 或 "post_tag")
 * 将 JSON 对象反序列化为正确的子类 (Category 或 Tag).
 */
public class TermDeserializer implements JsonDeserializer<Term> {

    @Override
    public Term deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        // 检查 'taxonomy' 字段是否存在且不为空
        if (jsonObject.has("taxonomy") && !jsonObject.get("taxonomy").isJsonNull()) {
            String taxonomy = jsonObject.get("taxonomy").getAsString();

            // 根据 taxonomy 的值来决定反序列化成哪个具体的子类
            if ("category".equals(taxonomy)) {
                return context.deserialize(jsonObject, Category.class);
            } else if ("post_tag".equals(taxonomy)) {
                return context.deserialize(jsonObject, Tag.class);
            }
        }

        return null;
    }
}