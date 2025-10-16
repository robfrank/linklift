import { useState, useEffect } from "react";
import api from "../services/api";

/**
 * Custom hook to fetch content for a link
 * @param {string|null} linkId - The link ID to fetch content for
 * @returns {{data: import('../types/content').ContentResponse|null, isLoading: boolean, error: Error|null, refetch: Function}}
 */
export const useContent = (linkId) => {
    const [data, setData] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);
    const [refetchTrigger, setRefetchTrigger] = useState(0);

    useEffect(() => {
        // Don't fetch if no linkId
        if (!linkId) {
            setData(null);
            setError(null);
            setIsLoading(false);
            return;
        }

        const fetchContent = async () => {
            setIsLoading(true);
            setError(null);

            try {
                const response = await api.getContent(linkId);
                setData(response);
            } catch (err) {
                setError(err);
                setData(null);
            } finally {
                setIsLoading(false);
            }
        };

        fetchContent();
    }, [linkId, refetchTrigger]);

    const refetch = () => {
        setRefetchTrigger((prev) => prev + 1);
    };

    return { data, isLoading, error, refetch };
};
