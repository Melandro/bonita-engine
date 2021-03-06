/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.profile.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.commons.LogUtil;
import org.bonitasoft.engine.events.EventActionType;
import org.bonitasoft.engine.events.EventService;
import org.bonitasoft.engine.events.model.SDeleteEvent;
import org.bonitasoft.engine.events.model.SInsertEvent;
import org.bonitasoft.engine.events.model.SUpdateEvent;
import org.bonitasoft.engine.events.model.builders.SEventBuilder;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.OrderByType;
import org.bonitasoft.engine.persistence.QueryOptions;
import org.bonitasoft.engine.persistence.ReadPersistenceService;
import org.bonitasoft.engine.persistence.SBonitaReadException;
import org.bonitasoft.engine.persistence.SBonitaSearchException;
import org.bonitasoft.engine.persistence.SelectByIdDescriptor;
import org.bonitasoft.engine.persistence.SelectListDescriptor;
import org.bonitasoft.engine.persistence.SelectOneDescriptor;
import org.bonitasoft.engine.profile.ProfileService;
import org.bonitasoft.engine.profile.SProfileCreationException;
import org.bonitasoft.engine.profile.SProfileDeletionException;
import org.bonitasoft.engine.profile.SProfileEntryCreationException;
import org.bonitasoft.engine.profile.SProfileEntryDeletionException;
import org.bonitasoft.engine.profile.SProfileEntryNotFoundException;
import org.bonitasoft.engine.profile.SProfileEntryUpdateException;
import org.bonitasoft.engine.profile.SProfileMemberCreationException;
import org.bonitasoft.engine.profile.SProfileMemberDeletionException;
import org.bonitasoft.engine.profile.SProfileMemberNotFoundException;
import org.bonitasoft.engine.profile.SProfileMemberReadException;
import org.bonitasoft.engine.profile.SProfileNotFoundException;
import org.bonitasoft.engine.profile.SProfileUpdateException;
import org.bonitasoft.engine.profile.builder.SProfileBuilder;
import org.bonitasoft.engine.profile.builder.SProfileBuilderAccessor;
import org.bonitasoft.engine.profile.builder.SProfileEntryBuilder;
import org.bonitasoft.engine.profile.builder.impl.SProfileBuilderAccessorImpl;
import org.bonitasoft.engine.profile.builder.impl.SProfileLogBuilder;
import org.bonitasoft.engine.profile.builder.impl.SProfileMemberLogBuilder;
import org.bonitasoft.engine.profile.model.SProfile;
import org.bonitasoft.engine.profile.model.SProfileEntry;
import org.bonitasoft.engine.profile.model.SProfileMember;
import org.bonitasoft.engine.profile.model.impl.SProfileMemberImpl;
import org.bonitasoft.engine.profile.persistence.SelectDescriptorBuilder;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLog;
import org.bonitasoft.engine.queriablelogger.model.SQueriableLogSeverity;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction;
import org.bonitasoft.engine.queriablelogger.model.builder.HasCRUDEAction.ActionType;
import org.bonitasoft.engine.queriablelogger.model.builder.SLogBuilder;
import org.bonitasoft.engine.queriablelogger.model.builder.SPersistenceLogBuilder;
import org.bonitasoft.engine.recorder.Recorder;
import org.bonitasoft.engine.recorder.SRecorderException;
import org.bonitasoft.engine.recorder.model.DeleteAllRecord;
import org.bonitasoft.engine.recorder.model.DeleteRecord;
import org.bonitasoft.engine.recorder.model.EntityUpdateDescriptor;
import org.bonitasoft.engine.recorder.model.InsertRecord;
import org.bonitasoft.engine.recorder.model.UpdateRecord;
import org.bonitasoft.engine.services.QueriableLoggerService;

/**
 * @author Matthieu Chaffotte
 * @author Elias Ricken de Medeiros
 */
public class ProfileServiceImpl implements ProfileService {

    private final ReadPersistenceService persistenceService;

    private final Recorder recorder;

    private final EventService eventService;

    private final SProfileBuilderAccessor profileBuilderAccessor;

    private final TechnicalLoggerService logger;

    private final QueriableLoggerService queriableLoggerService;

    public ProfileServiceImpl(final ReadPersistenceService persistenceService, final Recorder recorder, final EventService eventService,
            final TechnicalLoggerService logger, final QueriableLoggerService queriableLoggerService) {
        super();
        this.persistenceService = persistenceService;
        this.recorder = recorder;
        this.eventService = eventService;
        profileBuilderAccessor = new SProfileBuilderAccessorImpl();
        this.logger = logger;
        this.queriableLoggerService = queriableLoggerService;
    }

    @Override
    public SProfileBuilderAccessor getSProfileBuilderAccessor() {
        return profileBuilderAccessor;
    }

    @Override
    public SProfile createProfile(final SProfile profile) throws SProfileCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "createProfile"));
        }
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.CREATED, "Adding a new profile with name " + profile.getName());
        final InsertRecord insertRecord = new InsertRecord(profile);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(PROFILE, EventActionType.CREATED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(PROFILE).setObject(profile).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_OK, logBuilder, "createProfile");

            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "createProfile"));
            }
            return profile;
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createProfile", re));
            }
            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "createProfile");
            throw new SProfileCreationException(re);
        }
    }

    @Override
    public SProfile getProfile(final long profileId) throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfile"));
        }
        try {
            final SelectByIdDescriptor<SProfile> descriptor = SelectDescriptorBuilder.getElementById(SProfile.class, "Profile", profileId);
            final SProfile profile = persistenceService.selectById(descriptor);
            if (profile == null) {
                throw new SProfileNotFoundException("No profile exists with id: " + profileId);
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfile"));
            }
            return profile;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfile", bre));
            }
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public SProfile getProfileByName(final String profileName) throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfileByName"));
        }
        try {
            final SelectOneDescriptor<SProfile> descriptor = SelectDescriptorBuilder.getElementByNameDescriptor(SProfile.class, "Profile", profileName);
            final SProfile profile = persistenceService.selectOne(descriptor);
            if (profile == null) {
                throw new SProfileNotFoundException("No profile exists with name: " + profileName);
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfileByName"));
            }
            return profile;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfileByName", bre));
            }
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public List<SProfile> getProfiles(final List<Long> profileIds) throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfiles"));
        }
        final List<SProfile> profiles = new ArrayList<SProfile>();
        for (final Long profileId : profileIds) {
            final SProfile profile = getProfile(profileId);
            profiles.add(profile);
        }
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfiles"));
        }
        return profiles;
    }

    @Override
    public void updateProfile(final SProfile profile, final EntityUpdateDescriptor descriptor) throws SProfileUpdateException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "updateProfile"));
        }
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.UPDATED, "Updating profile with name " + profile.getName());
        final SProfileBuilder sProfileBuilder = profileBuilderAccessor.getSProfileBuilder();
        final SProfile oldUser = sProfileBuilder.createNewInstance(profile).done();
        final UpdateRecord updateRecord = UpdateRecord.buildSetFields(profile, descriptor);
        SUpdateEvent updateEvent = null;
        if (eventService.hasHandlers(PROFILE, EventActionType.UPDATED)) {
            updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(PROFILE).setObject(profile).done();
            updateEvent.setOldObject(oldUser);
        }
        try {
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateProfile");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "updateProfile"));
            }
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "updateProfile", re));
            }

            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateProfile");
            throw new SProfileUpdateException(re);
        }
    }

    @Override
    public void deleteProfile(final SProfile profile) throws SProfileNotFoundException, SProfileDeletionException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfile"));
        }
        final int batchNumber = 1000;
        List<SProfileEntry> entries;
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.DELETED, "Deleting profile with name " + profile.getName());
        final DeleteRecord deleteRecord = new DeleteRecord(profile);
        SDeleteEvent deleteEvent = null;
        if (eventService.hasHandlers(PROFILE, EventActionType.DELETED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(PROFILE).setObject(profile).done();
        }
        try {
            do {
                entries = getEntriesOfProfile(profile.getId(), 0, batchNumber);
                for (final SProfileEntry entry : entries) {
                    deleteProfileEntry(entry);
                }
            } while (!entries.isEmpty());

            List<SProfileMember> profileUsers;
            do {
                profileUsers = getSProfileMembers(profile.getId());
                for (final SProfileMember profileUser : profileUsers) {
                    deleteProfileMember(profileUser);
                }
            } while (!profileUsers.isEmpty());
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteProfile");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfile"));
            }
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteProfile", re));
            }
            initiateLogBuilder(profile.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteProfile");
            throw new SProfileDeletionException(re);
        } catch (final SProfileEntryDeletionException spede) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteProfile", spede));
            }
            throw new SProfileDeletionException(spede);
        } catch (final SProfileMemberDeletionException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteProfile", e));
            }
            throw new SProfileDeletionException(e);
        }
    }

    @Override
    public void deleteProfile(final long profileId) throws SProfileNotFoundException, SProfileDeletionException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfile"));
        }
        final SProfile profile = getProfile(profileId);
        this.deleteProfile(profile);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfile"));
        }
    }

    private SProfileLogBuilder getSProfileLog(final ActionType actionType, final String message) {
        final SProfileLogBuilder logBuilder = new SProfileLogBuilder();
        this.initializeLogBuilder(logBuilder, message);
        this.updateLog(actionType, logBuilder);
        return logBuilder;
    }

    private SProfileMemberLogBuilder getProfileMemberLog(final ActionType actionType, final String message) {
        final SProfileMemberLogBuilder logBuilder = new SProfileMemberLogBuilder();
        this.initializeLogBuilder(logBuilder, message);
        this.updateLog(actionType, logBuilder);
        return logBuilder;
    }

    private <T extends SLogBuilder> void initializeLogBuilder(final T logBuilder, final String message) {
        logBuilder.createNewInstance().actionStatus(SQueriableLog.STATUS_FAIL).severity(SQueriableLogSeverity.INTERNAL).rawMessage(message);
    }

    private <T extends HasCRUDEAction> void updateLog(final ActionType actionType, final T logBuilder) {
        logBuilder.setActionType(actionType);
    }

    @Override
    public SProfileEntry getProfileEntry(final long profileEntryId) throws SProfileEntryNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfileEntry"));
        }
        try {
            final SelectByIdDescriptor<SProfileEntry> descriptor = SelectDescriptorBuilder.getElementById(SProfileEntry.class, "ProfileEntry", profileEntryId);
            final SProfileEntry profileEntry = persistenceService.selectById(descriptor);
            if (profileEntry == null) {
                throw new SProfileEntryNotFoundException("No entry exists with id: " + profileEntryId);
            }
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfileEntry"));
            }
            return profileEntry;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfileEntry", bre));
            }
            throw new SProfileEntryNotFoundException(bre);
        }
    }

    @Override
    public List<SProfileEntry> getEntriesOfProfile(final long profileId, final int fromIndex, final int numberOfProfileEntries)
            throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getEntriesOfProfile"));
        }
        try {
            final List<SProfileEntry> listsProfileEntries = persistenceService.selectList(SelectDescriptorBuilder.getEntriesOfProfile(profileId, fromIndex,
                    numberOfProfileEntries));
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getEntriesOfProfile"));
            }
            return listsProfileEntries;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getEntriesOfProfile", bre));
            }
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public List<SProfileEntry> getEntriesOfProfile(final long profileId, final int fromIndex, final int numberOfProfileEntries, final String field,
            final OrderByType order) throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getEntriesOfProfile"));
        }
        try {
            final List<SProfileEntry> listspEntries = persistenceService.selectList(SelectDescriptorBuilder.getEntriesOfProfile(profileId, field, order,
                    fromIndex,
                    numberOfProfileEntries));
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getEntriesOfProfile"));
            }
            return listspEntries;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getEntriesOfProfile", bre));
            }
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public List<SProfileEntry> getEntriesOfProfileByParentId(final long profileId, final long parentId, final int fromIndex, final int numberOfProfileEntries,
            final String field, final OrderByType order) throws SProfileNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getEntriesOfProfileByParentId"));
        }
        try {
            final List<SProfileEntry> listspEntries = persistenceService.selectList(SelectDescriptorBuilder.getEntriesOfProfile(profileId, parentId, field,
                    order,
                    fromIndex, numberOfProfileEntries));
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getEntriesOfProfileByParentId"));
            }
            return listspEntries;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getEntriesOfProfileByParentId", bre));
            }
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public SProfileEntry createProfileEntry(final SProfileEntry profileEntry) throws SProfileEntryCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "createProfileEntry"));
        }
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.CREATED, "Adding a new pofile entry with name " + profileEntry.getName());
        final InsertRecord insertRecord = new InsertRecord(profileEntry);
        SInsertEvent insertEvent = null;
        if (eventService.hasHandlers(PROFILE, EventActionType.CREATED)) {
            final SEventBuilder eventBuilder = eventService.getEventBuilder();
            insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(ENTRY_PROFILE).setObject(profileEntry).done();
        }
        try {
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_OK, logBuilder, "createProfileEntry");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "createProfileEntry"));
            }
            return profileEntry;
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "createProfileEntry", re));
            }
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "createProfileEntry");
            throw new SProfileEntryCreationException(re);
        }
    }

    @Override
    public void updateProfileEntry(final SProfileEntry profileEntry, final EntityUpdateDescriptor descriptor) throws SProfileEntryUpdateException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "updateProfileEntry"));
        }
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.UPDATED, "Updating profile entry with name " + profileEntry.getName());
        try {
            final SProfileEntryBuilder sProfileBuilder = profileBuilderAccessor.getSProfileEntryBuilder();
            final SProfileEntry oldProfileEntry = sProfileBuilder.createNewInstance(profileEntry).done();
            final UpdateRecord updateRecord = UpdateRecord.buildSetFields(profileEntry, descriptor);
            SUpdateEvent updateEvent = null;
            if (eventService.hasHandlers(ENTRY_PROFILE, EventActionType.UPDATED)) {
                updateEvent = (SUpdateEvent) eventService.getEventBuilder().createUpdateEvent(ENTRY_PROFILE).setObject(profileEntry).done();
                updateEvent.setOldObject(oldProfileEntry);
            }
            recorder.recordUpdate(updateRecord, updateEvent);
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_OK, logBuilder, "updateProfileEntry");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "updateProfileEntry"));
            }
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "updateProfileEntry", re));
            }
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "updateProfileEntry");
            throw new SProfileEntryUpdateException(re);
        }
    }

    @Override
    public void deleteProfileEntry(final SProfileEntry profileEntry) throws SProfileEntryDeletionException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfileEntry"));
        }
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.DELETED, "Deleting profile entry with name " + profileEntry.getName());
        try {
            final DeleteRecord deleteRecord = new DeleteRecord(profileEntry);
            SDeleteEvent deleteEvent = null;
            if (eventService.hasHandlers(ENTRY_PROFILE, EventActionType.DELETED)) {
                final SEventBuilder eventBuilder = eventService.getEventBuilder();
                deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(ENTRY_PROFILE).setObject(profileEntry).done();
            }
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteProfileEntry");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfileEntry"));
            }
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteProfileEntry", re));
            }
            initiateLogBuilder(profileEntry.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteProfileEntry");
            throw new SProfileEntryDeletionException(re);
        }
    }

    @Override
    public void deleteProfileEntry(final long profileEntryId) throws SProfileEntryNotFoundException, SProfileEntryDeletionException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfileEntry"));
        }
        final SProfileEntry profileEntry = getProfileEntry(profileEntryId);
        deleteProfileEntry(profileEntry);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfileEntry"));
        }
    }

    private SProfileMemberImpl getProfileMember(final long profileId, final String displayNamePart1, final String displayNamePart2,
            final String displayNamePart3) {
        final SProfileMemberImpl profileMember = new SProfileMemberImpl(profileId);
        profileMember.setDisplayNamePart1(displayNamePart1);
        profileMember.setDisplayNamePart2(displayNamePart2);
        profileMember.setDisplayNamePart1(displayNamePart3);
        return profileMember;
    }

    @Override
    public SProfileMember addUserToProfile(final long profileId, final long userId, final String firstName, final String lastName, final String userName)
            throws SProfileMemberCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "addUserToProfile"));
        }
        final SProfileMemberImpl profileMember = getProfileMember(profileId, firstName, lastName, userName);
        profileMember.setUserId(userId);
        createProfileMember(profileMember);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "addUserToProfile"));
        }
        return profileMember;
    }

    private void createProfileMember(final SProfileMemberImpl profileMember) throws SProfileMemberCreationException {
        final String message = "Adding a new profile member for userId " + profileMember.getUserId() + " with roleId " + profileMember.getRoleId()
                + " in groupId " + profileMember.getGroupId();
        final SProfileLogBuilder logBuilder = getSProfileLog(ActionType.CREATED, message);
        try {
            final InsertRecord insertRecord = new InsertRecord(profileMember);
            SInsertEvent insertEvent = null;
            if (eventService.hasHandlers(PROFILE, EventActionType.CREATED)) {
                final SEventBuilder eventBuilder = eventService.getEventBuilder();
                insertEvent = (SInsertEvent) eventBuilder.createInsertEvent(PROFILE_MEMBER).setObject(profileMember).done();
            }
            recorder.recordInsert(insertRecord, insertEvent);
            initiateLogBuilder(profileMember.getId(), SQueriableLog.STATUS_OK, logBuilder, "insertProfileMember");
        } catch (final SRecorderException re) {
            initiateLogBuilder(profileMember.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "insertProfileMember");
            throw new SProfileMemberCreationException(re);
        }
    }

    @Override
    public SProfileMember addGroupToProfile(final long profileId, final long groupId, final String groupName, final String parentPath)
            throws SProfileMemberCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "addGroupToProfile"));
        }
        final SProfileMemberImpl profileMember = getProfileMember(profileId, groupName, parentPath, null);
        profileMember.setGroupId(groupId);
        createProfileMember(profileMember);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "addGroupToProfile"));
        }
        return profileMember;
    }

    @Override
    public SProfileMember addRoleToProfile(final long profileId, final long roleId, final String roleName) throws SProfileMemberCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "addRoleToProfile"));
        }
        final SProfileMemberImpl profileMember = getProfileMember(profileId, roleName, null, null);
        profileMember.setRoleId(roleId);
        createProfileMember(profileMember);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "addRoleToProfile"));
        }
        return profileMember;
    }

    @Override
    public SProfileMember addRoleAndGroupToProfile(final long profileId, final long roleId, final long groupId, final String roleName, final String groupName,
            final String groupParentPath) throws SProfileMemberCreationException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "addRoleAndGroupToProfile"));
        }
        final SProfileMemberImpl profileMember = getProfileMember(profileId, roleName, groupName, groupParentPath);
        profileMember.setGroupId(groupId);
        profileMember.setRoleId(roleId);
        createProfileMember(profileMember);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "addRoleAndGroupToProfile"));
        }
        return profileMember;
    }

    @Override
    public void deleteProfileMember(final long profileMemberId) throws SProfileMemberDeletionException, SProfileMemberNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfileMember"));
        }
        final SProfileMember profileMember = getProfileMemberWithoutDisplayName(profileMemberId);
        deleteProfileMember(profileMember);
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfileMember"));
        }
    }

    private SProfileMember getProfileMemberWithoutDisplayName(final long profileMemberId) throws SProfileMemberNotFoundException {
        final SelectByIdDescriptor<SProfileMember> selectByIdDescriptor = SelectDescriptorBuilder.getProfileMemberWithoutDisplayName(profileMemberId);
        try {
            final SProfileMember profileMember = persistenceService.selectById(selectByIdDescriptor);
            if (profileMember == null) {
                throw new SProfileMemberNotFoundException(profileMemberId + " does not refer to any profile member");
            }
            return profileMember;
        } catch (final SBonitaReadException bre) {
            throw new SProfileMemberNotFoundException(bre);
        }
    }

    @Override
    public void deleteProfileMember(final SProfileMember profileMember) throws SProfileMemberDeletionException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "deleteProfileMember"));
        }
        final String message = "Deleting profile member for userId " + profileMember.getUserId() + " with roleId " + profileMember.getRoleId() + " in groupId "
                + profileMember.getGroupId();
        final SProfileMemberLogBuilder logBuilder = getProfileMemberLog(ActionType.DELETED, message);
        try {
            final DeleteRecord deleteRecord = new DeleteRecord(profileMember);
            SDeleteEvent deleteEvent = null;
            if (eventService.hasHandlers(PROFILE_MEMBER, EventActionType.DELETED)) {
                final SEventBuilder eventBuilder = eventService.getEventBuilder();
                deleteEvent = (SDeleteEvent) eventBuilder.createDeleteEvent(PROFILE_MEMBER).setObject(profileMember).done();
            }
            recorder.recordDelete(deleteRecord, deleteEvent);
            initiateLogBuilder(profileMember.getId(), SQueriableLog.STATUS_OK, logBuilder, "deleteProfileMember");
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "deleteProfileMember"));
            }
        } catch (final SRecorderException re) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "deleteProfileMember", re));
            }

            initiateLogBuilder(profileMember.getId(), SQueriableLog.STATUS_FAIL, logBuilder, "deleteProfileMember");
            throw new SProfileMemberDeletionException(re);
        }
    }

    @Override
    public List<SProfileMember> getProfileMembersOfUser(final long userId, final int fromIndex, final int numberOfElements, final String field,
            final OrderByType order) throws SProfileMemberReadException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfileMembersOfUser"));
        }
        try {
            final SelectListDescriptor<SProfileMember> descriptor = SelectDescriptorBuilder.getDirectProfileMembersOfUser(userId, field, order, fromIndex,
                    numberOfElements);
            final List<SProfileMember> listspProfileMembers = persistenceService.selectList(descriptor);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfileMembersOfUser"));
            }
            return listspProfileMembers;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfileMembersOfUser", bre));
            }
            throw new SProfileMemberReadException(bre);
        }
    }

    @Override
    public List<SProfileMember> getProfileMembersOfGroup(final long groupId, final int fromIndex, final int numberOfElements, final String field,
            final OrderByType order) throws SProfileMemberReadException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfileMembersOfGroup"));
        }
        try {
            final SelectListDescriptor<SProfileMember> descriptor = SelectDescriptorBuilder.getDirectProfileMembersOfGroup(groupId, field, order, fromIndex,
                    numberOfElements);
            final List<SProfileMember> listspProfileMembers = persistenceService.selectList(descriptor);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfileMembersOfGroup"));
            }
            return listspProfileMembers;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfileMembersOfGroup", bre));
            }
            throw new SProfileMemberReadException(bre);
        }
    }

    @Override
    public List<SProfileMember> getProfileMembersOfRole(final long roleId, final int fromIndex, final int numberOfElements, final String field,
            final OrderByType order) throws SProfileMemberReadException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfileMembersOfRole"));
        }
        try {
            final SelectListDescriptor<SProfileMember> descriptor = SelectDescriptorBuilder.getDirectProfileMembersOfRole(roleId, field, order, fromIndex,
                    numberOfElements);
            final List<SProfileMember> listspProfileMembers = persistenceService.selectList(descriptor);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfileMembersOfRole"));
            }
            return listspProfileMembers;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfileMembersOfRole", bre));
            }
            throw new SProfileMemberReadException(bre);
        }
    }

    @Override
    public List<SProfile> getProfilesOfUser(final long userId) throws SProfileMemberNotFoundException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getProfilesOfUser"));
        }
        final SelectListDescriptor<SProfile> descriptor = SelectDescriptorBuilder.getProfilesOfUser(userId);
        try {
            final List<SProfile> listspProfiles = persistenceService.selectList(descriptor);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getProfilesOfUser"));
            }
            return listspProfiles;
        } catch (final SBonitaReadException bre) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getProfilesOfUser", bre));
            }
            throw new SProfileMemberNotFoundException(bre);
        }
    }

    @Override
    public List<SProfileMember> getSProfileMembers(final long profileId) throws SProfileNotFoundException {
        try {
            final SelectListDescriptor<SProfileMember> descriptor = SelectDescriptorBuilder.getSProfileMembersWithoutDisplayName(profileId);
            return persistenceService.selectList(descriptor);
        } catch (final SBonitaReadException bre) {
            throw new SProfileNotFoundException(bre);
        }
    }

    @Override
    public List<SProfileMember> searchProfileMembers(final String querySuffix, final QueryOptions queryOptions) throws SBonitaSearchException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "searchProfileMembers"));
        }
        try {
            final List<SProfileMember> listSProfileMembers = persistenceService.searchEntity(SProfileMember.class, querySuffix, queryOptions, null);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "searchProfileMembers"));
            }
            return listSProfileMembers;
        } catch (final Exception e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "searchProfileMembers", e));
            }
            throw new SBonitaSearchException(e);
        }

    }

    @Override
    public long getNumberOfProfileMembers(final String querySuffix, final QueryOptions countOptions) throws SBonitaSearchException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getNumberOfProfileMembers"));
        }
        try {
            final long number = persistenceService.getNumberOfEntities(SProfileMember.class, querySuffix, countOptions, null);
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getNumberOfProfileMembers"));
            }
            return number;
        } catch (final Exception e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getNumberOfProfileMembers", e));
            }
            throw new SBonitaSearchException(e);
        }

    }

    @Override
    public List<SProfileMember> getNumberOfProfileMembers(final List<Long> profileIds) throws SBonitaSearchException {
        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
            logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogBeforeMethod(this.getClass(), "getNumberOfProfileMembers"));
        }
        if (profileIds == null || profileIds.size() == 0) {
            return Collections.emptyList();
        }
        try {
            final Map<String, Object> emptyMap = Collections.singletonMap("profileIds", (Object) profileIds);
            final List<SProfileMember> results = persistenceService.selectList(new SelectListDescriptor<SProfileMember>("getProfileMembersFromProfileIds",
                    emptyMap,
                    SProfileMember.class));
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogAfterMethod(this.getClass(), "getNumberOfProfileMembers"));
            }
            return results;
        } catch (final SBonitaReadException e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.TRACE)) {
                logger.log(this.getClass(), TechnicalLogSeverity.TRACE, LogUtil.getLogOnExceptionMethod(this.getClass(), "getNumberOfProfileMembers", e));
            }
            throw new SBonitaSearchException(e);
        }
    }

    private void initiateLogBuilder(final long objectId, final int sQueriableLogStatus, final SPersistenceLogBuilder logBuilder, final String callerMethodName) {
        logBuilder.actionScope(String.valueOf(objectId));
        logBuilder.actionStatus(sQueriableLogStatus);
        logBuilder.objectId(objectId);
        final SQueriableLog log = logBuilder.done();
        if (queriableLoggerService.isLoggable(log.getActionType(), log.getSeverity())) {
            queriableLoggerService.log(this.getClass().getName(), callerMethodName, log);
        }
    }

    @Override
    public void deleteAllProfileMembers() throws SProfileMemberDeletionException {
        try {
            final DeleteAllRecord record = new DeleteAllRecord(SProfileMember.class, null);
            recorder.recordDeleteAll(record);
        } catch (final SRecorderException e) {
            throw new SProfileMemberDeletionException("Can't delete all profile members.", e);
        }
    }

    @Override
    public long getNumberOfProfiles(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.emptyMap();
            return persistenceService.getNumberOfEntities(SProfile.class, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProfile> searchProfiles(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.emptyMap();
            return persistenceService.searchEntity(SProfile.class, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    public List<SProfileMember> getProfileMembers(final int fromIndex, final int numberOfElements, final String field, final OrderByType order)
            throws SBonitaReadException {
        final SelectListDescriptor<SProfileMember> descriptor = SelectDescriptorBuilder.getProfileMembers(field, order, fromIndex, numberOfElements);
        return persistenceService.selectList(descriptor);
    }

    @Override
    public long getNumberOfProfileEntries(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.emptyMap();
            return persistenceService.getNumberOfEntities(SProfileEntry.class, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

    @Override
    public List<SProfileEntry> searchProfileEntries(final QueryOptions queryOptions) throws SBonitaSearchException {
        try {
            final Map<String, Object> parameters = Collections.emptyMap();
            return persistenceService.searchEntity(SProfileEntry.class, queryOptions, parameters);
        } catch (final SBonitaReadException e) {
            throw new SBonitaSearchException(e);
        }
    }

}
