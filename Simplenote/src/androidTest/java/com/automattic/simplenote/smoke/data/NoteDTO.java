package com.automattic.simplenote.smoke.data;

import java.util.List;

public class NoteDTO {
    private String title;
    private String content;
    private List<String> tags;

    public NoteDTO(String title, String content, List<String> tags) {
        this.title = title;
        this.content = content;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }


    public List<String> getTags() {
        return tags;
    }
}
