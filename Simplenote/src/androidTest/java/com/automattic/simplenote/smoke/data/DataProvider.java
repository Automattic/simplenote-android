package com.automattic.simplenote.smoke.data;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataProvider {
    public static final String LOGIN_EMAIL = "test1234@simplenote.com";
    public static final String LOGIN_PASSWORD = "12341234";
    public static final String LOGIN_WRONG_PASSWORD = "1234";

    public static List<NoteDTO> generateNotes(int size) {
        List<NoteDTO> noteDTOList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NoteDTO note = new NoteDTO("Corona Days " + i, "The moment that people need to support each other on these dooms days!", Arrays.asList("Corona"));
            noteDTOList.add(note);
        }
        return noteDTOList;
    }
}
