import React from "react";
import { Box, Chip, Tooltip } from "@mui/material";
import { LocalOffer } from "@mui/icons-material";
import { Tag } from "../../../../domain/models/Tag";

interface TagChipsProps {
  tags: Tag[];
  onRemove?: (tagId: string) => void;
  size?: "small" | "medium";
}

export const TagChips: React.FC<TagChipsProps> = ({ tags, onRemove, size = "small" }) => {
  if (tags.length === 0) return null;

  return (
    <Box display="flex" flexWrap="wrap" gap={0.5} alignItems="center">
      {tags.map((tag) => (
        <Tooltip key={tag.id} title={`Tag: ${tag.name}`}>
          <Chip
            icon={<LocalOffer sx={{ fontSize: "0.7rem !important" }} />}
            label={tag.name}
            size={size}
            onDelete={onRemove ? () => onRemove(tag.id) : undefined}
            sx={{
              height: size === "small" ? 20 : 28,
              fontSize: size === "small" ? "0.65rem" : "0.75rem",
              "& .MuiChip-icon": { fontSize: "0.7rem" }
            }}
            color="secondary"
            variant="outlined"
          />
        </Tooltip>
      ))}
    </Box>
  );
};
