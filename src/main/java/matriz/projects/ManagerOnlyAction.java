package matriz.projects;

/** The actions only a project's manager may take, with the exact refusal message of each (DoD04). */
public enum ManagerOnlyAction {

    /** US09 AC07. */
    MANAGE_MEMBERS("Only the project manager can manage members."),
    /** US14 AC04. */
    EDIT_PROJECT("Only the project manager can edit the project."),
    /** US15 AC04. */
    DELETE_PROJECT("Only the project manager can delete the project.");

    private final String refusal;

    ManagerOnlyAction(String refusal) {
        this.refusal = refusal;
    }

    public String refusal() {
        return refusal;
    }
}
