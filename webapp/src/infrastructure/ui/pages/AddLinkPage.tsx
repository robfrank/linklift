import React, { useEffect } from "react";
import { useNavigate } from "react-router-dom";
// @ts-ignore
import { useSnackbar } from "../../../contexts/SnackbarContext";
import { useLinks } from "../hooks/useLinks";
import { AddLink } from "../components/AddLink";

const AddLinkPage = () => {
  const navigate = useNavigate();
  const { showSnackbar } = useSnackbar();
  const { addLink, isAddingLink, addLinkError, resetAddLinkError } = useLinks();

  useEffect(() => {
    if (addLinkError) {
      showSnackbar(addLinkError, "error");
      resetAddLinkError();
    }
  }, [addLinkError, showSnackbar, resetAddLinkError]);

  const handleSubmit = async (data: { url: string; title: string; description: string }) => {
    try {
      await addLink(data);
      showSnackbar("Link added successfully!", "success");
      setTimeout(() => {
        navigate("/");
      }, 1500);
    } catch (e) {
      // Error managed by effect
    }
  };

  return <AddLink onSubmit={handleSubmit} onCancel={() => navigate("/")} loading={isAddingLink} />;
};

export default AddLinkPage;
