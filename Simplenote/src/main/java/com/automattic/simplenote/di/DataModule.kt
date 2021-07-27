package com.automattic.simplenote.di

import android.content.Context
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.SimperiumTagsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.simperium.client.Bucket
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    fun providesTagsBucket(@ApplicationContext appContext: Context): Bucket<Tag> = (appContext as Simplenote).tagsBucket

    @Provides
    fun providesNotesBucket(@ApplicationContext appContext: Context): Bucket<Note> = (appContext as Simplenote).notesBucket

    @Provides
    fun providesTagsRepository(tagsBucket: Bucket<Tag>, notesBucket: Bucket<Note>): TagsRepository =
        SimperiumTagsRepository(tagsBucket, notesBucket)
}