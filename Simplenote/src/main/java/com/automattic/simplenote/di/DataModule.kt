package com.automattic.simplenote.di

import android.content.Context
import com.automattic.simplenote.Simplenote
import com.automattic.simplenote.models.Note
import com.automattic.simplenote.models.Tag
import com.automattic.simplenote.repositories.CollaboratorsRepository
import com.automattic.simplenote.repositories.SimperiumCollaboratorsRepository
import com.automattic.simplenote.repositories.SimperiumTagsRepository
import com.automattic.simplenote.repositories.TagsRepository
import com.simperium.client.Bucket
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    companion object {
        @Provides
        fun providesTagsBucket(simplenote: Simplenote): Bucket<Tag> = simplenote.tagsBucket

        @Provides
        fun providesNotesBucket(simplenote: Simplenote): Bucket<Note> = simplenote.notesBucket
    }

    @Binds
    abstract fun bindsTagsRepository(repository: SimperiumTagsRepository): TagsRepository

    @Binds
    abstract fun bindsCollaboratorsRepository(repository: SimperiumCollaboratorsRepository): CollaboratorsRepository
}
