/**
 * Copyright (C) 2011-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.api;

import java.util.List;
import java.util.Map;

import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.RetrieveException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.UpdateException;
import org.bonitasoft.engine.identity.ContactData;
import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.identity.UserCreator;
import org.bonitasoft.engine.identity.UserCriterion;
import org.bonitasoft.engine.identity.UserNotFoundException;
import org.bonitasoft.engine.identity.UserUpdater;
import org.bonitasoft.engine.search.SearchOptions;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.session.InvalidSessionException;

/**
 * @author Feng Hui
 * @author Matthieu Chaffotte
 * @author Celine Souchet
 */
public interface UserAPI {

    /**
     * Creates a user.
     * <b>
     * The password can be empty.
     * 
     * @param userName
     *            the name of the user
     * @param password
     *            the password of the user
     * @return the created user
     * @throws AlreadyExistsException
     *             If the name is already taken by an existing user
     * @throws CreationException
     *             If an exception occurs during the user creation
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User createUser(String userName, String password) throws AlreadyExistsException, CreationException;

    /**
     * Creates a user.
     * <b>
     * The password can be empty.
     * 
     * @param userName
     *            the name of the user
     * @param password
     *            the password of the user
     * @param firstName
     *            the first name of the user
     * @param lastName
     *            the last name of the user
     * @return the created user
     * @throws AlreadyExistsException
     *             If the name is already taken by an existing user
     * @throws CreationException
     *             If an exception occurs during the user creation
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User createUser(String userName, String password, String firstName, String lastName) throws AlreadyExistsException, CreationException;

    /**
     * Creates a user.
     * <b>
     * It takes the values of the creator in order to create the user.
     * 
     * @param creator
     *            the user to create
     * @return the created user
     * @throws AlreadyExistsException
     *             If the name is already taken by an existing user
     * @throws CreationException
     *             If an exception occurs during the user creation
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User createUser(UserCreator creator) throws AlreadyExistsException, CreationException;

    /**
     * Updates the user according to the updater values.
     * 
     * @param userId
     *            the identifier of the user
     * @param updater
     *            the user updater
     * @return the updated user
     * @throws UserNotFoundException
     *             If the user identifier does not refer to an existing user
     * @throws UpdateException
     *             If an exception occurs during the user update
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User updateUser(long userId, UserUpdater updater) throws UserNotFoundException, UpdateException;

    /**
     * Deletes the user.
     * 
     * @param userId
     *            the identifier of the user
     * @throws DeletionException
     *             If an exception occurs during the user deletion
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    void deleteUser(long userId) throws DeletionException;

    /**
     * Deletes the user.
     * 
     * @param userName
     *            the name of the user
     * @throws DeletionException
     *             If an exception occurs during the user deletion
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    void deleteUser(String userName) throws DeletionException;

    /**
     * Deletes the users.
     * 
     * @param userIds
     *            the identifiers of the users
     * @throws DeletionException
     *             If an exception occurs during the user deletion
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    void deleteUsers(List<Long> userIds) throws DeletionException;

    /**
     * Retrieves the user.
     * <b>
     * It throws a {@link UserNotFoundException} if the user identifier equals the technical user identifier (-1).
     * 
     * @param userId
     *            the identifier of the user
     * @return the searched user
     * @throws UserNotFoundException
     *             If the user identifier does not refer to an existing user
     * @throws RetrieveException
     *             If an exception occurs during the user retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User getUser(long userId) throws UserNotFoundException;

    /**
     * Retrieves the user.
     * 
     * @param userName
     *            the name of the user
     * @return the role
     * @throws UserNotFoundException
     *             If the user name does not refer to an existing user
     * @throws RetrieveException
     *             If an exception occurs during the role retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    User getUserByUserName(String userName) throws UserNotFoundException;

    /**
     * Retrieves the contact data (personal or professional) of the user.
     * 
     * @param userId
     *            the identifier of the user
     * @param personal
     *            true if the contact data is the personal one
     * @return the contact data
     * @throws UserNotFoundException
     *             If the user name does not refer to an existing user
     * @throws RetrieveException
     *             If an exception occurs during the role retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    ContactData getUserContactData(long userId, boolean personal) throws UserNotFoundException;

    /**
     * Returns the total number of users.
     * 
     * @return the total number of users
     * @throws RetrieveException
     *             If an exception occurs during the count retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    long getNumberOfUsers();

    /**
     * Retrieves the paginated list of users. It retrieves from the startIndex to the startIndex + maxResults.
     * 
     * @param startIndex
     *            the start index
     * @param maxResults
     *            the max number of users
     * @param criterion
     *            the sorting criterion
     * @return the paginated list of users
     * @throws RetrieveException
     *             If an exception occurs during the user retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    List<User> getUsers(int startIndex, int maxResults, UserCriterion criterion);

    /**
     * Retrieves the users. The map contains the couples userId/User.
     * <b>
     * If a user does not exists, no exception is thrown and no value is added in the map.
     * 
     * @param userIds
     *            the identifiers of the users
     * @return the users
     * @throws RetrieveException
     *             If an exception occurs during the user retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    Map<Long, User> getUsers(List<Long> userIds);

    /**
     * Searches users according to the criteria containing in the options.
     * 
     * @param options
     *            the search criteria
     * @return the search result
     * @throws SearchException
     *             If an exception occurs during the user searching
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    SearchResult<User> searchUsers(SearchOptions options) throws SearchException;

    /**
     * Returns the total number of users of the role.
     * 
     * @param roleId
     *            the identifier of the role
     * @return the total number of users of the role
     * @throws RetrieveException
     *             If an exception occurs during the count retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    long getNumberOfUsersInRole(long roleId);

    /**
     * Retrieves the paginated list of roles.
     * <b>
     * It retrieves from the startIndex to the startIndex + maxResults.
     * 
     * @param roleId
     *            the identifier of the role
     * @param startIndex
     *            the start index
     * @param maxResults
     *            the max number of roles
     * @param criterion
     *            the sorting criterion
     * @return the paginated list of roles
     * @throws RetrieveException
     *             If an exception occurs during the role retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    List<User> getUsersInRole(long roleId, int startIndex, int maxResults, UserCriterion criterion);

    /**
     * Returns the total number of users of the group.
     * 
     * @param groupId
     *            the identifier of the group
     * @return the total number of users of the group
     * @throws RetrieveException
     *             If an exception occurs during the count retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    long getNumberOfUsersInGroup(long groupId) throws BonitaException;

    /**
     * Retrieves the paginated list of groups.
     * <b>
     * It retrieves from the startIndex to the startIndex + maxResults.
     * 
     * @param groupId
     *            the identifier of the group
     * @param startIndex
     *            the start index
     * @param maxResults
     *            the max number of groups
     * @param criterion
     *            the sorting criterion
     * @return the paginated list of groups
     * @throws RetrieveException
     *             If an exception occurs during the group retrieving
     * @throws InvalidSessionException
     *             If the session is invalid (expired, unknown, ...)
     * @since 6.0
     */
    List<User> getUsersInGroup(long groupId, int startIndex, int maxResults, UserCriterion criterion);

}
