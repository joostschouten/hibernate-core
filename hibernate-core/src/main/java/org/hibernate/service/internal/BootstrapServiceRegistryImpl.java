/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.service.internal;

import java.util.LinkedHashSet;

import org.hibernate.integrator.internal.IntegratorServiceImpl;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.integrator.spi.IntegratorService;
import org.hibernate.service.Service;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.service.classloading.spi.ClassLoaderService;
import org.hibernate.service.spi.ServiceBinding;
import org.hibernate.service.spi.ServiceException;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

/**
 * {@link ServiceRegistry} implementation containing specialized "bootstrap" services, specifically:<ul>
 * <li>{@link ClassLoaderService}</li>
 * <li>{@link IntegratorService}</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class BootstrapServiceRegistryImpl implements ServiceRegistryImplementor, ServiceBinding.OwningRegistry {
	private static final LinkedHashSet<Integrator> NO_INTEGRATORS = new LinkedHashSet<Integrator>();

	private final ServiceBinding<ClassLoaderService> classLoaderServiceBinding;
	private final ServiceBinding<IntegratorService> integratorServiceBinding;

	public static Builder builder() {
		return new Builder();
	}

	public BootstrapServiceRegistryImpl() {
		this( new ClassLoaderServiceImpl(), NO_INTEGRATORS );
	}

	public BootstrapServiceRegistryImpl(
			ClassLoaderService classLoaderService,
			IntegratorService integratorService) {
		this.classLoaderServiceBinding = new ServiceBinding<ClassLoaderService>(
				this,
				ClassLoaderService.class,
				classLoaderService
		);

		this.integratorServiceBinding = new ServiceBinding<IntegratorService>(
				this,
				IntegratorService.class,
				integratorService
		);
	}


	public BootstrapServiceRegistryImpl(
			ClassLoaderService classLoaderService,
			LinkedHashSet<Integrator> providedIntegrators) {
		this( classLoaderService, new IntegratorServiceImpl( providedIntegrators, classLoaderService ) );
	}



	@Override
	public <R extends Service> R getService(Class<R> serviceRole) {
		final ServiceBinding<R> binding = locateServiceBinding( serviceRole );
		return binding == null ? null : binding.getService();
	}

	@Override
	@SuppressWarnings( {"unchecked"})
	public <R extends Service> ServiceBinding<R> locateServiceBinding(Class<R> serviceRole) {
		if ( ClassLoaderService.class.equals( serviceRole ) ) {
			return (ServiceBinding<R>) classLoaderServiceBinding;
		}
		else if ( IntegratorService.class.equals( serviceRole ) ) {
			return (ServiceBinding<R>) integratorServiceBinding;
		}

		return null;
	}

	@Override
	public void destroy() {
	}

	@Override
	public ServiceRegistry getParentServiceRegistry() {
		return null;
	}

	@Override
	public <R extends Service> R initiateService(ServiceInitiator<R> serviceInitiator) {
		// the bootstrap registry should currently be made up of only directly built services.
		throw new ServiceException( "Boot-strap registry should only contain directly built services" );
	}

	public static class Builder {
		private final LinkedHashSet<Integrator> providedIntegrators = new LinkedHashSet<Integrator>();
		private ClassLoader applicationClassLoader;
		private ClassLoader resourcesClassLoader;
		private ClassLoader hibernateClassLoader;
		private ClassLoader environmentClassLoader;

		public Builder with(Integrator integrator) {
			providedIntegrators.add( integrator );
			return this;
		}

		public Builder withApplicationClassLoader(ClassLoader classLoader) {
			this.applicationClassLoader = classLoader;
			return this;
		}

		public Builder withResourceClassLoader(ClassLoader classLoader) {
			this.resourcesClassLoader = classLoader;
			return this;
		}

		public Builder withHibernateClassLoader(ClassLoader classLoader) {
			this.hibernateClassLoader = classLoader;
			return this;
		}

		public Builder withEnvironmentClassLoader(ClassLoader classLoader) {
			this.environmentClassLoader = classLoader;
			return this;
		}

		public BootstrapServiceRegistryImpl build() {
			final ClassLoaderServiceImpl classLoaderService = new ClassLoaderServiceImpl(
					applicationClassLoader,
					resourcesClassLoader,
					hibernateClassLoader,
					environmentClassLoader
			);

			final IntegratorServiceImpl integratorService = new IntegratorServiceImpl(
					providedIntegrators,
					classLoaderService
			);

			return new BootstrapServiceRegistryImpl( classLoaderService, integratorService );
		}
	}
}
