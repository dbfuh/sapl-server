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
package io.sapl.server.ce.ui.views.pdpconfig;

import java.util.Optional;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import io.sapl.server.ce.model.pdpconfiguration.DuplicatedVariableNameException;
import io.sapl.server.ce.model.pdpconfiguration.InvalidJsonException;
import io.sapl.server.ce.model.pdpconfiguration.InvalidVariableNameException;
import io.sapl.server.ce.model.pdpconfiguration.Variable;
import io.sapl.server.ce.model.pdpconfiguration.VariablesService;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import io.sapl.server.ce.ui.views.MainLayout;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RolesAllowed("ADMIN")
@RequiredArgsConstructor
@PageTitle("Edit Variable")
@Route(value = EditVariableView.ROUTE, layout = MainLayout.class)
public class EditVariableView extends VerticalLayout implements HasUrlParameter<Long> {
	public static final String ROUTE = "pdp-config/edit-variable";

	private final VariablesService variableService;

	private long variableId;

	private TextField nameTextField = new TextField("Variable Name");
// TODO: 	private JsonEditor jsonEditor;
	private TextField jsonEditor   = new TextField("Variable Value");
	private Button    saveButton   = new Button("Save");
	private Button    cancelButton = new Button("Cancel");

	/**
	 * The {@link Variable} to edit.
	 */
	private Variable variable;

	@PostConstruct
	private void init() {
		add(nameTextField, jsonEditor, new HorizontalLayout( cancelButton,saveButton));
	}

	@Override
	public void setParameter(BeforeEvent event, Long parameter) {
		variableId = parameter;

		reloadVariable();
		addListener();

		addAttachListener((__) -> {
			if (variable == null) {
				log.warn("variable with id {} is not existing, redirect to list view", parameter);
				getUI().ifPresent(ui -> ui.navigate(PDPConfigView.ROUTE));
			}
		});
	}

	private void reloadVariable() {
		Optional<Variable> optionalVariable = variableService.getById(variableId);
		if (optionalVariable.isEmpty()) {
			// Vaadin UI object is not available yet, redirect to list view via attach
			// listener
			return;
		}

		variable = optionalVariable.get();

		setUI();
	}

	/**
	 * Imports the previously set instance of {@link Variable} to the UI.
	 */
	private void setUI() {
		nameTextField.setValue(variable.getName());
		jsonEditor.setValue(variable.getJsonValue());
	}

	private void addListener() {
		saveButton.addClickListener(clickEvent -> {
			String name      = nameTextField.getValue();
			String jsonValue = jsonEditor.getValue();

			try {
				variableService.edit(variableId, name, jsonValue);
			} catch (InvalidJsonException ex) {
				log.error("cannot edit variable due to invalid json", ex);
				ErrorNotificationUtils.show("The value of the variable contains invalid JSON.");
				return;
			} catch (InvalidVariableNameException ex) {
				log.error("cannot create variable due to invalid name", ex);
				ErrorNotificationUtils.show(String.format("The name is invalid (min length: %d, max length: %d).",
						VariablesService.MIN_NAME_LENGTH, VariablesService.MAX_NAME_LENGTH));
				return;
			} catch (DuplicatedVariableNameException ex) {
				log.error("cannot edit variable due to duplicated name", ex);
				ErrorNotificationUtils.show("The name is already used by another variable.");
				return;
			}

			cancelButton.getUI().ifPresent(ui -> ui.navigate(PDPConfigView.ROUTE));
		});

		cancelButton.addClickListener(clickEvent -> getUI().ifPresent(ui -> ui.navigate(PDPConfigView.ROUTE)));
	}

}
