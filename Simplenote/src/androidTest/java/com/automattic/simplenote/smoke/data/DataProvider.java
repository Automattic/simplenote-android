package com.automattic.simplenote.smoke.data;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DataProvider {
    public static final String LOGIN_EMAIL = "test1234@simplenote.com";
    public static final String UNKNOWN_EMAIL = "@";
    //    public static final String LOGIN_PASSWORD = "12341234";
    public static final String LOGIN_WRONG_PASSWORD = "1234";

    public static List<NoteDTO> generateNotes(int size) {
        List<NoteDTO> noteDTOList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NoteDTO note = new NoteDTO("Corona Days " + getUniqueToken(), "Corona Days... People need to support each other on these dooms days!", Arrays.asList("Corona"));
            noteDTOList.add(note);
        }
        return noteDTOList;
    }

    public static List<NoteDTO> generateNotesWithUniqueContent(int size) {
        List<NoteDTO> noteDTOList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            NoteDTO note = new NoteDTO("Corona Days " + getUniqueToken(), getUniqueToken() + " Corona Days... People need to support each other on these dooms days!", Arrays.asList("Corona"));
            noteDTOList.add(note);
        }
        return noteDTOList;
    }

    private static String getUniqueToken() {
        return String.valueOf(System.currentTimeMillis());
    }
}
