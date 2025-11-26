/*
 * Class: CreateFolder
 *
 * File: mtext/examples/CreateFolder.java
 *
 * Copyright (c) 2003 kuehn & weyh Software GmbH
 * Freiburg, Germany
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of kuehn & weyh Software GmbH ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with kuehn & weyh.
 */

package fr.kw.api.resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;

import de.kwsoft.mtext.api.DataModelResourceInformation;
import de.kwsoft.mtext.api.MTextException;
import de.kwsoft.mtext.api.MTextInstanceClosedException;
import de.kwsoft.mtext.api.Resource;
import de.kwsoft.mtext.api.ResourceInformation;
import de.kwsoft.mtext.api.ResourceNotFoundException;
import de.kwsoft.mtext.api.ResourceProvider;
import de.kwsoft.mtext.api.ResourceProvider.ResourceType;
import de.kwsoft.mtext.api.client.MTextClient;
import de.kwsoft.mtext.api.client.MTextFactory;
import de.kwsoft.mtext.api.server.MTextServer;
import de.kwsoft.mtext.resource.ResourceInfo;
import fr.utils.configuration.BaseConfiguration;

/**
 * M/Text client API example: Create a new folder in a specified parent folder
 * 
 * @author Michal Michal
 **/
public class KWResources {
	/**
	 * Creates a new folder in the specified parent folder
	 * 
	 * @param args Command line arguments<br>
	 *             args[0] = username<br>
	 *             args[1] = password<br>
	 *             args[2] = baseProject or "null" for all projects<br>
	 *             args[3] = resource type or "null" for all resources<br>
	 **/
	public static void main(String[] args) {
		// initializations
		MTextClient client = null;
		MTextServer server = null;

		// if there are two less or to much arguments
		if (args.length == 4) {
			final String name = args[0];
			final String pwd = args[1];
			final String baseProject = "null".equals(args[2]) ? null : args[2];
			ResourceType resourceType = "null".equals(args[3]) ? null : ResourceType.valueOf(args[3]);

			Collection<ResourceInformation> clientRess = Collections.EMPTY_LIST;
			Collection<ResourceInformation> serverRess = Collections.EMPTY_LIST;

			try {

				// connect - client
				client = MTextFactory.connect(name, pwd, null);

				{
					System.out.println("");
					System.out.println("*** CLIENT ***");
					System.out.flush();

					ResourceProvider resourceProvider = client.getResourceProvider();
					Collection<ResourceInformation> resources = resourceProvider.listResources(baseProject, null,
							resourceType);

					System.out.println("found " + resources.size() + " files");
					for (ResourceInformation resourceInformation : resources) {

						System.out.print(" > " + resourceInformation.getFullName());
						System.out.print(" (" + resourceInformation.getProjectName() + ")");

						File file = new File("resources/" + resourceInformation.getProjectRelativeName() + "/");
						if (!file.getName().startsWith(".")) {
							try {
								System.out.println(resourceInformation.getDescription());
								Resource resource = resourceProvider.loadResource(resourceInformation.getFullName());

								Field[] fields = resourceInformation.getClass().getDeclaredFields();
								for (Field m : fields) {

									System.out.println(m.toGenericString());
									System.out.println(m.toGenericString());
									m.setAccessible(true);
									Object value = m.get(resourceInformation);
									if (value instanceof ResourceInfo) {
										String hash = ((ResourceInfo) value).getContentHash();
										System.out.println("hash=" + hash);
									}
								}

								file.getParentFile().mkdirs();

								FileOutputStream fos = new FileOutputStream(file);
								InputStream is = resource.getStream();
								IOUtils.copy(is, fos);
								fos.close();
								is.close();
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}

						if (resourceType == ResourceType.DATA_MODEL) {
							System.out.print(", DATA_MODEL: "
									+ ((DataModelResourceInformation) resourceInformation).loadDataModel().getName());
						}
						System.out.println("");
					}

					clientRess = resources;
				}

				// connect - server
				server = de.kwsoft.mtext.api.server.MTextFactory.connect(name, pwd, null);
				{
					System.out.println("");
					System.out.println("*** SERVER ***");
					System.out.flush();

					ResourceProvider resourceProvider = server.getResourceProvider();
					Collection<ResourceInformation> resources = resourceProvider.listResources(baseProject, null,
							resourceType);

					System.out.println("found " + resources.size() + " files");
					for (ResourceInformation resourceInformation : resources) {
						System.out.print(" > " + resourceInformation.getFullName());
						System.out.print(" (" + resourceInformation.getProjectName() + ")");
						if (resourceType == ResourceType.DATA_MODEL) {
							System.out.print(", DATA_MODEL: "
									+ ((DataModelResourceInformation) resourceInformation).loadDataModel().getName());
						}
						System.out.println("");
					}

					serverRess = resources;
				}

			}

			// M/Text exception
			catch (MTextException me) {
				System.out.println("Invalid search " + args[3] + " in " + baseProject + " project!");
				me.printStackTrace();
			}
			// e.g. the path is wrong -> the folder is null -> Exception
			catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (client != null) {
					// close client
					client.close();
				}
				if (server != null) {
					// close server
					server.close();
				}
			}

			boolean resourceCountEquals = clientRess.size() == serverRess.size();
			List<String> clientFullNames = new ArrayList();
			List<String> serverFullNames = new ArrayList();
			Set<String> clientDoubles = new HashSet();
			Set<String> serverDoubles = new HashSet();

			for (ResourceInformation resourceInformation : serverRess) {
				String fullName = resourceInformation.getFullName();
				if (serverFullNames.contains(fullName)) {
					serverDoubles.add(fullName);
				}
				serverFullNames.add(fullName);
			}

			for (ResourceInformation resourceInformation : clientRess) {
				String fullName = resourceInformation.getFullName();
				if (clientFullNames.contains(fullName)) {
					clientDoubles.add(fullName);
				}
				clientFullNames.add(fullName);
				if (serverFullNames.contains(fullName)) {
					serverFullNames.remove(fullName);
				}
			}

			for (ResourceInformation resourceInformation : serverRess) {
				String fullName = resourceInformation.getFullName();
				if (clientFullNames.contains(fullName)) {
					clientFullNames.remove(fullName);
				}
			}

			System.out.println("************************************************");
			System.out.println("client and server resource count equals: " + resourceCountEquals);
			System.out.println("client+: " + clientFullNames.size() + ", " + clientFullNames);
			System.out.println("server+: " + serverFullNames.size() + ", " + serverFullNames);
			System.out.println("client duplicity: " + clientDoubles);
			System.out.println("server duplicity: " + serverDoubles);

		} else {
			System.out.println("M/Text client api example: ListResources");
			System.out.println();
			System.out.println(
					"Usage: java mtext.examples.ListResources " + "<name> <password> <base project> <resource type>");
		}
	}

	private BaseConfiguration configuration;
	private MTextServer mText;

	public KWResources(BaseConfiguration configuration) throws MTextException {
		this.configuration = configuration;
		Properties connectParams = new Properties();
		connectParams.put("kwsoft.env.mtextclient.mtext.communication.EJB.ProviderUrl",
				configuration.getUrlForJavaAPI());

		try {
			mText = de.kwsoft.mtext.api.server.MTextFactory.connect(configuration.getKWUser(),
					configuration.getKWPlainPassword(), connectParams);
		} catch (MTextException e) {

			throw new MTextException("Illegal argument Exception : " + e.getMessage(), e);
		}
	}

	public Resource getResource(ResourceInformation resourceInfo) throws MTextException {
		try {
			return mText.getResourceProvider().loadResource(resourceInfo.getFullName());
		} catch (MTextInstanceClosedException | ResourceNotFoundException | IllegalArgumentException
				| NullPointerException e) {
			throw new MTextException("Can't get resource '" + resourceInfo.getFullName() + "' : " + e.getMessage(), e);
		}
	}

	public String getResourceHash(ResourceInformation resourceInfo) {
		Field[] fields = resourceInfo.getClass().getDeclaredFields();
		for (Field m : fields) {
			m.setAccessible(true);
			Object value;
			try {
				value = m.get(resourceInfo);
				if (value instanceof ResourceInfo) {
					String hash = ((ResourceInfo) value).getContentHash();
					return hash;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {

			}

		}
		return null;
	}

	public Collection<ResourceInformation> listProjectResources(String project) throws MTextException {
		try {

			ResourceProvider resourceProvider = mText.getResourceProvider();
			Collection<ResourceInformation> resources = resourceProvider.listResources(project, null, null);
			return resources;

		} catch (IllegalArgumentException e) {
			throw new MTextException("Illegal argument Exception : " + e.getMessage(), e);
		}

	}
}
