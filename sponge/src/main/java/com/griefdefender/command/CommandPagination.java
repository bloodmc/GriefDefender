package com.griefdefender.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Description;
import com.griefdefender.internal.pagination.ActivePagination;
import com.griefdefender.internal.pagination.GDPaginationHolder;
import net.kyori.text.TextComponent;
import net.kyori.text.adapter.spongeapi.TextAdapter;
import net.kyori.text.format.TextColor;
import org.spongepowered.api.command.CommandSource;

public class CommandPagination extends BaseCommand {

    @CommandAlias("gd:pagination")
    @Description("Used internally by GD for pagination purposes.")
    public void execute(CommandSource src, String[] args) throws CommandException {
        String id = args[0];
        final ActivePagination activePagination = GDPaginationHolder.getInstance().getActivePagination(src, id);
        if (activePagination == null) {
            TextAdapter.sendComponent(src, TextComponent.of("Source " + src.getName() + " has no paginations!", TextColor.RED));
            return;
        }
        String action = args[1];
        if (action.equals("page")) {
            activePagination.currentPage();
        } else if (action.equals("next")) {
            activePagination.nextPage();
        } else if (action.equals("prev")) {
            activePagination.previousPage();
        } else {
            int page = Integer.parseInt(action);
            activePagination.specificPage(page);
        }
    }
}
