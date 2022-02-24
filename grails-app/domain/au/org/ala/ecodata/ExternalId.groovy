package au.org.ala.ecodata

import groovy.transform.EqualsAndHashCode

/**
 * Associates an id held in an external system with a Project
 */
@EqualsAndHashCode
class ExternalId implements Comparable {

    enum IdType { INTERNAL_ORDER_NUMBER, SERVICE_ONE, WORK_ORDER }

    static constraints = {
    }

    IdType idType
    String externalId

    @Override
    int compareTo(Object otherId) {
        ExternalId other = (ExternalId)otherId
        return (idType.ordinal()+externalId).compareTo(other?.idType?.ordinal()+other?.externalId)
    }
}