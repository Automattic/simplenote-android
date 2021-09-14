package com.automattic.simplenote.repositories

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimperiumCollaboratorsRepositoryTest {

    private val collaboratorsRepository = SimperiumCollaboratorsRepository()

    @Test
    fun validEmailsShouldBeValidCollaborator() {
        assertTrue(collaboratorsRepository.isValidCollaborator("name1@test.com"))
        // Email with + sign
        assertTrue(collaboratorsRepository.isValidCollaborator("name12.lastname+503@test.com"))
        // IP address
        assertTrue(collaboratorsRepository.isValidCollaborator("name12_lastname+503@192.168.43.54"))
        assertTrue(collaboratorsRepository.isValidCollaborator("name12-lastname+503@192.168.43.54"))
        // Email top level domain
        assertTrue(collaboratorsRepository.isValidCollaborator("name@example.co.jp"))
    }

    @Test
    fun invalidEmailsShouldBeInvalidCollaborator() {
        // Email without complete domain
        assertFalse(collaboratorsRepository.isValidCollaborator("name@test"))
        assertFalse(collaboratorsRepository.isValidCollaborator("d"))
        // Valid URL but not email
        assertFalse(collaboratorsRepository.isValidCollaborator("nametest.com"))
        // With subdomain
        assertFalse(collaboratorsRepository.isValidCollaborator("sub.test@test"))
        // With double @
        assertFalse(collaboratorsRepository.isValidCollaborator("test@@test.com"))
        assertFalse(collaboratorsRepository.isValidCollaborator("あいうえお@example.com"))
        assertFalse(collaboratorsRepository.isValidCollaborator("just”not”right@example.com"))
    }
}
