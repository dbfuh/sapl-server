/*
 * Copyright © 2017-2021 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.server.ce.ui.views.clientcredentials;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.vaadin.lineawesome.LineAwesomeIcon;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.CallbackDataProvider;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.sapl.server.ce.model.clients.ClientCredentials;
import io.sapl.server.ce.model.clients.ClientCredentialsService;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import io.sapl.server.ce.ui.views.MainLayout;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import reactor.util.function.Tuple2;

@RolesAllowed("ADMIN")
@RequiredArgsConstructor
@PageTitle("Client Credentials")
@Route(value = ClientCredentialsView.ROUTE, layout = MainLayout.class)
public class ClientCredentialsView extends VerticalLayout {
	public static final String ROUTE = "clients";

	private final ClientCredentialsService clientCredentialsService;

	private Grid<ClientCredentials> clientCredentialsGrid = new Grid<>();
	private Button                  createButton          = new Button("New Client");

	@PostConstruct
	private void init() {
		add(createButton, clientCredentialsGrid);

		createButton.addClickListener(e -> createClient());

		initClientCredentialsGrid();
	}

	private void createClient() {
		Tuple2<ClientCredentials, String> clientCredentialsWithSecret;
		try {
			clientCredentialsWithSecret = clientCredentialsService.createDefault();
		} catch (Throwable throwable) {
			ErrorNotificationUtils.show("The client cannot be created due to an internal error.");
			return;
		}
		showDialogForCreatedVariable(clientCredentialsWithSecret.getT1().getKey(),
				clientCredentialsWithSecret.getT2());
		clientCredentialsGrid.getDataProvider().refreshAll();
	}

	private void initClientCredentialsGrid() {
		clientCredentialsGrid.addColumn(ClientCredentials::getKey).setHeader("Key").setSortable(true);

		clientCredentialsGrid.addComponentColumn(currentClientCredential -> {
			Button deleteButton = new Button("Delete", LineAwesomeIcon.TRASH_SOLID.create());
			deleteButton.setThemeName("primary");
			deleteButton.addClickListener(clickEvent -> deleteClient(currentClientCredential));

			HorizontalLayout componentsForEntry = new HorizontalLayout();
			componentsForEntry.add(deleteButton);

			return componentsForEntry;
		});

		// set data provider
		CallbackDataProvider<ClientCredentials, Void> dataProvider = DataProvider.fromCallbacks(query -> {
			Stream<ClientCredentials> stream = clientCredentialsService.getAll().stream();

			Optional<Comparator<ClientCredentials>> optionalCompatator = query.getSortingComparator();
			if (optionalCompatator.isPresent()) {
				stream = stream.sorted(optionalCompatator.get());
			}

			return stream.skip(query.getOffset()).limit(query.getLimit());
		}, query -> (int) clientCredentialsService.getAmount());
		clientCredentialsGrid.setItems(dataProvider);
	}

	private void deleteClient(ClientCredentials currentClientCredential) {
		ConfirmUtils
				.letConfirm("Delete Client",
						String.format("Should the client credentials with key \"%s\" really be deleted?",
								currentClientCredential.getKey()),
						() -> executeDeletionOfClient(currentClientCredential), () -> {
						});
	}

	private void executeDeletionOfClient(ClientCredentials currentClientCredential) {
		long idOfClientToRemove = currentClientCredential.getId();
		try {
			clientCredentialsService.delete(idOfClientToRemove);
		} catch (Throwable throwable) {
			ErrorNotificationUtils
					.show("The client cannot be deleted due to an internal error.");
			return;
		}
		clientCredentialsGrid.getDataProvider().refreshAll();
	}

	private void showDialogForCreatedVariable(@NonNull String key, @NonNull String secret) {
		var layout = new VerticalLayout();
		var text   = new Span(
				"A new client has been created. The following secret will only be shown once and is not recoverable. Make sure to write it down.");

		var keyField = new TextField("Client Key");
		keyField.setValue(key);
		keyField.setReadOnly(true);
		keyField.setWidthFull();
		var secretField = new TextField("Client Secret");
		secretField.setValue(secret);
		secretField.setReadOnly(true);
		secretField.setWidthFull();

		layout.add(text, keyField, secretField);

		Dialog dialog = new Dialog(layout);
		dialog.setHeaderTitle("Client Created");
		var closeButton = new Button(new Icon("lumo", "cross"), e -> dialog.close());
		closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
		dialog.getHeader().add(closeButton);
		dialog.setWidth("600px");
		dialog.setModal(true);
		dialog.setCloseOnEsc(false);
		dialog.setCloseOnOutsideClick(false);
		dialog.open();
	}

}
