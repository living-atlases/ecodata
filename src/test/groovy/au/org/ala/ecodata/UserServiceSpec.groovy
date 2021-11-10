package au.org.ala.ecodata

import grails.test.mongodb.MongoSpec
import grails.testing.services.ServiceUnitTest

/**
 * We are extending the mongo spec as one of the main things we need to test are complex queries on
 * the User collection, which are potentially different in GORM for Mongo vs GORM
 */
class UserServiceSpec extends MongoSpec implements ServiceUnitTest<UserService> {

    def setup() {
        User.findAll().each{it.delete(flush:true)}
        Hub.findAll().each{it.delete(flush:true)}
        new Hub(hubId:'h1', urlPath:"hub1").save(flush:true, failOnError:true)
        new Hub(hubId:'h2', urlPath:'hub2').save(flush:true, failOnError:true)
    }

    def cleanup() {
        User.findAll().each{it.delete(flush:true)}
        Hub.findAll().each{it.delete(flush:true)}
    }

    def "The recordLoginTime method requires a hubId and userId to be supplied"() {
        when:
        service.recordLoginTime(null, "user1")

        then:
        thrown(IllegalArgumentException)

        when:
        service.recordLoginTime("hub1", null)

        then:
        thrown(IllegalArgumentException)
    }

    def "The Hub associated with the supplied hubId must exist"() {
        when:
        service.recordLoginTime("h3", "user1")

        then:
        thrown(IllegalArgumentException)
    }

    def "The service provides a convenience method to record the time a user logged into a hub"() {
        setup:
        String hubId = "h1"
        String userId = "u1"
        new Hub(hubId:hubId).save()
        Date loginTime1 = DateUtil.parse("2021-01-01T00:00:00Z")
        Date loginTime2 = DateUtil.parse("2021-01-01T00:00:00Z")

        when: "No User document exists, this method will insert one"
        User user = service.recordLoginTime(hubId, userId, loginTime1)

        then:
        user.userId == userId
        user.getUserHub(hubId).hubId == hubId
        user.getUserHub(hubId).lastLoginTime == loginTime1

        when: "If the hub doesn't exist, the method will add one"
        user = service.recordLoginTime("h2", userId, loginTime2)

        then:
        user.userId == userId
        user.userHubs.size() == 2
        user.getUserHub("h2").lastLoginTime == loginTime2

        when: "The last login time is changed, it is updated correctly"
        user = service.recordLoginTime(hubId, userId, loginTime2)

        then:
        user.userId == userId
        user.userHubs.size() == 2
        user.getUserHub(hubId).lastLoginTime == loginTime2
        user.getUserHub("h2").lastLoginTime == loginTime2

    }

    def "The service can return a list of users who haven't logged into a hub after a specified time"(String hubId, String queryDate, int expectedResultCount) {
        setup:
        insertUserLogin("u1", "h1", "2021-01-01T00:00:00Z")
        insertUserLogin("u1", "h2", "2021-02-01T00:00:00Z")
        insertUserLogin("u2", "h1", "2021-01-10T00:00:00Z")
        insertUserLogin("u3", "h1", "2021-05-01T00:00:00Z")
        insertUserLogin("u4", "h1", "2021-06-01T00:00:00Z")
        User.withSession {session -> session.flush()}

        when:
        Date date = DateUtil.parse(queryDate)
        List users = service.findUsersNotLoggedInToHubSince(hubId, date)

        then:
        users.size() == expectedResultCount

        where:
        hubId | queryDate              | expectedResultCount
        "h1"  | "2021-02-01T00:00:00Z" | 2
        "h2"  | "2021-02-01T00:00:00Z" | 0
        "h2"  | "2021-02-01T00:00:01Z" | 1
        "h1"  | "2021-05-15T00:00:00Z" | 3
    }

    private void insertUserLogin(String userId, String hubId, String loginTime) {
        Date date = DateUtil.parse(loginTime)
        service.recordLoginTime(hubId, userId, date)
    }

}
