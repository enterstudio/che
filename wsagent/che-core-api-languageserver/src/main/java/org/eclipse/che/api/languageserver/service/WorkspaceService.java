/*******************************************************************************
 * Copyright (c) 2012-2017 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.api.languageserver.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.languageserver.exception.LanguageServerException;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistry;
import org.eclipse.che.api.languageserver.registry.LanguageServerRegistryImpl;
import org.eclipse.che.api.languageserver.server.dto.DtoServerImpls.SymbolInformationDto;
import org.eclipse.che.api.languageserver.shared.model.ExtendedWorkspaceSymbolParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.services.LanguageServer;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

/**
 * REST API for the workspace/* services defined in https://github.com/Microsoft/vscode-languageserver-protocol
 * Dispatches onto the {@link LanguageServerRegistryImpl}.
 *
 * @author Evgen Vidolob
 */
@Singleton
@Path("languageserver/workspace")
public class WorkspaceService {
    private LanguageServerRegistry registry;

    @Inject
    public WorkspaceService(LanguageServerRegistry registry) {
        this.registry = registry;
    }

    @POST
    @Path("symbol")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<? extends SymbolInformationDto> documentSymbol(ExtendedWorkspaceSymbolParams workspaceSymbolParams)
            throws ExecutionException,
                   InterruptedException,
                   LanguageServerException {
        LanguageServer server = getServer(TextDocumentService.prefixURI(workspaceSymbolParams.getFileUri()));
        if (server == null) {
            return emptyList();
        }

        List<? extends SymbolInformation> informations = server.getWorkspaceService().symbol(workspaceSymbolParams).get();
        informations.forEach(o -> {
            Location location = o.getLocation();
            location.setUri(TextDocumentService.removePrefixUri(location.getUri()));
        });
        return informations.stream().map(o -> new SymbolInformationDto(o)).collect(Collectors.toList());
    }

    private LanguageServer getServer(String uri) throws LanguageServerException {
        return registry.findServer(uri);
    }
}
