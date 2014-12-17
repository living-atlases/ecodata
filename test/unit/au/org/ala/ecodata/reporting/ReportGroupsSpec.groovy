package au.org.ala.ecodata.reporting


import spock.lang.Specification;

/**
 * Tests for the ReportGroup classes
 */
public class ReportGroupsSpec extends Specification {

    def "grouping on a single property returns the value of the property as the group"(propertyName, data) {

        given:
        ReportGroups.DiscreteGroup groupingStrategy = new ReportGroups.DiscreteGroup(propertyName)

        when:
        def group = groupingStrategy.group(data)

        then:
        group == 'group1'

        where:

        propertyName | data
        'simple'               | [simple:'group1']
        'nested.property'      | [nested:[property:'group1']]
        'more.nested.property' | [more:[nested:[property:'group1']]]
    }


    def "grouping on a missing property should return null"(propertyName, data) {

        given:
        ReportGroups.DiscreteGroup groupingStrategy = new ReportGroups.DiscreteGroup(propertyName)

        when:
        def group = groupingStrategy.group(data)

        then:
        group == null

        where:

        propertyName | data
        'simple'               | [:]
        'nested.property'      | [nested:[missing:'group1']]
        'more.nested.property' | [more:[nested:[missing:'group1']]]
    }


    def "groups can be defined as half open buckets #data"(buckets, result, data) {
        given:
        ReportGroups.HistogramGroup groupingStrategy = new ReportGroups.HistogramGroup('simple', buckets)

        when:
        def group = groupingStrategy.group(data)

        then:
        group == result

        where:

        buckets   | result   | data
        [1,2,3,4] | '[1,2)'  | [simple:1]
        [1,2,3,4] | '[1,2)'  | [simple:1.1]
        [1,2,3,4] | '[2,3)'  | [simple:2]
        [1,2,3,4] | '[-,1)'  | [simple:0.5]
        [1,2,3,4] | '[4,-)'  | [simple:4]
        [1,2,3,4] | '[4,-)'  | [simple:5]

    }

    def "the grouping mechanism can be used to produce a single score by filtering on a single group value #value"(value, expected) {
        given:
        ReportGroups.FilteredGroup filteredGroup = new ReportGroups.FilteredGroup('simple', 'value1')

        when:
        def group = filteredGroup.group(['simple':value])

        then:
        group == expected

        where:

        value    | expected
        'value1' | Aggregator.DEFAULT_GROUP
        'value2' | null
        null     | null

    }

}
