package com.eypa.app.api;

import com.eypa.app.model.ContentItem;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class VideoEpisodeListDeserializer implements JsonDeserializer<List<ContentItem.VideoEpisode>> {

    @Override
    public List<ContentItem.VideoEpisode> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<ContentItem.VideoEpisode> list = new ArrayList<>();

        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            for (JsonElement element : array) {
                ContentItem.VideoEpisode episode = context.deserialize(element, ContentItem.VideoEpisode.class);
                list.add(episode);
            }
        } else if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            String jsonString = json.getAsString();
        }

        return list;
    }
}
