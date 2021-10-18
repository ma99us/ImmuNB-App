package org.maggus.myhealthnb.api.dto;

public interface Filterable<T> {

    /**
     * Returns a cloned DTO without some secure fields populated.
     * @return a redacted copy of DTO, safe to share with public.
     */
    T filter();
}
