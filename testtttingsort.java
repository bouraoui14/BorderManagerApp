
    List<? extends SortKey> sortKeys = getSortKeys();
    if (sortKeys == null || sortKeys.isEmpty()) {
        return;
    }

+   // ✔ Defensive copy
    List<Object> rows = new ArrayList<>(getModel().getDisplayRows());

+   // ✔ Parent → children map (always re-created)
    Map<GroupRow, List<Object>> parentChildMap = new HashMap<>();

+   // ✔ Always register parents
    for (Object row : rows) {
        if (row instanceof GroupRow) {
            parentChildMap.put((GroupRow) row, new ArrayList<>());
        }
    }

    List<Object> topLevelParents = new ArrayList<>();

+   // ✔ Phase 1: collect parents (only parents)
    for (Object row : rows) {
        if (row instanceof GroupRow) {
            topLevelParents.add(row);
        }
    }

+   // ✔ Phase 2: ALWAYS collect children, whether expanded or not
+   //   → Fixes the "orphan children when expanded" problem
    for (int i = 0; i < rows.size(); i++) {
        Object row = rows.get(i);

        if (row instanceof GroupRow) {
            continue; // children only
        }

        // find the nearest parent above
        GroupRow parent = null;
        for (int j = i - 1; j >= 0; j--) {
            Object prev = rows.get(j);
            if (prev instanceof GroupRow) {
                parent = (GroupRow) prev;
                break;
            }
        }

+       // ✔ Prevent NPE if child is before any parent
        if (parent != null) {
            parentChildMap.get(parent).add(row);
        }
    }

+   // ✔ SAFER METHOD: Evaluate values using safe comparator
    Comparator<Object> fullComparator = (o1, o2) -> {
        for (SortKey sortKey : sortKeys) {
            int col = sortKey.getColumn();

+           // ✔ Protect from index lookup errors
            int idx1 = safeModelIndex(o1);
            int idx2 = safeModelIndex(o2);
            if (idx1 < 0 || idx2 < 0) continue;

+           // ✔ Protect from null model values
            Object v1 = safeValue(idx1, col);
            Object v2 = safeValue(idx2, col);

+           // ✔ Compare safely using nullsLast + toString protection
            Comparator<Object> comp = Comparator.nullsLast(
                Comparator.comparing(x -> x == null ? "" : x.toString())
            );

            int result = comp.compare(v1, v2);
            if (result != 0) {
                return sortKey.getSortOrder() == SortOrder.ASCENDING ? result : -result;
            }
        }
        return 0;
    };

+   // ✔ Sort parents using safe comparator
    Collections.sort(topLevelParents, fullComparator);

+   // ✔ Sort children inside each parent
    for (Object parentObj : topLevelParents) {
        if (parentObj instanceof GroupRow) {
            List<Object> children = parentChildMap.get((GroupRow) parentObj);
+           if (children != null) {
                Collections.sort(children, fullComparator);
+           }
        }
    }

+   // ✔ Build the final sorted rows including expanded children under parent
    List<Object> finalRows = new ArrayList<>();
    for (Object parentObj : topLevelParents) {
        finalRows.add(parentObj);

        if (parentObj instanceof GroupRow) {
            GroupRow gp = (GroupRow) parentObj;

+           // ✔ If expanded, insert sorted children under parent
            if (gp.isExpanded()) {
                finalRows.addAll(parentChildMap.getOrDefault(gp, Collections.emptyList()));
            }
        }
    }

+   // ✔ Atomic update to model
    getModel().getDisplayRows().clear();
    getModel().getDisplayRows().addAll(finalRows);

    getModel().fireTableDataChanged();
}