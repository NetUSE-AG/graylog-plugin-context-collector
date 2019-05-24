/*
 * Copyright (C) NetUSE AG - All rights reserved
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */

import PropTypes from 'prop-types';
import * as Immutable from 'immutable';

import React, { useCallback, useContext, useEffect, useState, useRef } from 'react';
import { Button } from 'components/bootstrap';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, DataTable } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import EditContextCollectionModal from './EditContextCollectionModal';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { DEFAULT_TIMERANGE } from 'views/Constants';

import { useStore } from 'stores/connect';
import { ConfigurationsActions, ConfigurationsStore } from 'stores/configurations/ConfigurationsStore';


const getDefaultContextCollection = () => {
    return {
             name: 'new_collection',
             type: 'collection',
             key_fields: [],
             value_fields: [],
             timeout: 10,
             drop_incomplete:false,
             enabled: true,
           };
};

const CONTEXT_COLLECTOR_CONFIG = "de.netuse.graylog.context_collector.config.PluginConfigurationV2";
const DefaultConfig = {collection_configurations: [], collection_configurations_path: "/etc/graylog/context_collector.json", load_configuration_from_file: false};

const NetuseContextCollectorConfig = (props) => {
 const { configuration } = useStore(ConfigurationsStore);
 const propConfig = configuration[CONTEXT_COLLECTOR_CONFIG];
  const [config, setConfig ]  = useState(DefaultConfig);
  const [fields, setFields ]  = useState();
  const [showConfigModal, setShowConfigModal ] = useState(false);
  const { data: fieldTypes } = useFieldTypes([], DEFAULT_TIMERANGE);

  useEffect(() => {
     if (propConfig) {
     	setConfig(propConfig);
     } 
  }, [propConfig]);

  useEffect(() => {
	if (fieldTypes) {
		setFields(Immutable.List(fieldTypes.map((f) => f.value)));
	}
  }, [fieldTypes]);

  if (!(config && fields && fields.size > 0)) {
    return null;
  }

  const _onContextCollectionUpdate = (id) => {
    return (state) => {
        const update = ObjectUtils.clone(config);
        update['collection_configurations'][id] = state;
        setConfig(update);
    }
  };

  const _onContextCollectionAdd = () => {
      const update = ObjectUtils.clone(config);
      update['collection_configurations'].push(getDefaultContextCollection());
      setConfig(update);

  };

  const _onContextCollectionDelete = (id) => {
       return () => {
          const update = ObjectUtils.clone(config);
          update['collection_configurations'].splice(id, 1);
          setConfig(update);
      }
  };

  const _onContextCollectionToggle = (id) => {
      return () => {
          const update = ObjectUtils.clone(config);
          update['collection_configurations'][id]['enabled'] = !config['collection_configurations'][id]['enabled'];
          setConfig(update);
      }
  };

  const headers =  ['Name', 'Key','Values', 'Timeout', 'Drop Incomplete', 'Actions'];
  const rowFormatter = (entity, idx) =>  {
      let rid = Math.floor((Math.random() * 100000000) + 1);
      let actions;
      if (entity.enabled) {
        actions = (<>
                                <Button
                                      style={{ marginRight: 5 }}
                                      bsStyle="info"
                                      bsSize="xs"
                                      onClick={_onContextCollectionToggle(idx)}
                                      disabled={config.load_configuration_from_file}

                                    >
                                      Disable
                                    </Button>

                                 <EditContextCollectionModal
                                       key={rid}
                                       name={entity.name}
                                       type={entity.type}
                                       key_fields={entity.key_fields}
                                       value_fields={entity.value_fields}
                                       timeout={entity.timeout}
                                       drop_incomplete={entity.drop_incomplete}
                                       create={false}
                                       update={_onContextCollectionUpdate(idx)}
                                       fields={fields}
                                       disabled={config.load_configuration_from_file}

                                   />
                                   <Button
                                      style={{ marginRight: 5 }}
                                      bsStyle="primary"
                                      bsSize="xs"
                                      onClick={_onContextCollectionDelete(idx)}
                                      disabled={config.load_configuration_from_file}
                                    >
                                      Delete
                                    </Button>
                                   </>);

      } else {
            actions = (<> <Button
                                                          style={{ marginRight: 5 }}
                                                          bsStyle="info"
                                                          bsSize="xs"
                                                          onClick={_onContextCollectionToggle(idx)}
                                                        >
                                                          Enable
                                                        </Button></>);
      }


      return (
        <tr key={rid}>
          <td>{entity.name}</td>
          <td>{entity.key_fields.join(', ')}</td>
          <td>{entity.value_fields.join(', ')}</td>
          <td>{entity.timeout}</td>
          <td>{entity.drop_incomplete ? 'Enabled' : 'Disabled'}</td>
          <td>
            {actions}
          </td>
        </tr>
      );
    };


  const _updateConfigField = (field, value) => {
    const update = ObjectUtils.clone(config);
    update[field] = value;
    setConfig(update);
  };

  const _onCheckboxClick = (field) => {
    return (e) => {
      _updateConfigField(field, e.target.checked);
    };
  };

  const _onUpdate = (field) => {
    return (e) => {
      _updateConfigField(field, e.target.value);
    };
  };

  const _openModal = () => {
    setShowConfigModal(true);
  };

  const _closeModal = () => {
    setShowConfigModal(false);
  };

  const _resetConfig = () => {
    // Reset to initial state when the modal is closed without saving.
    setConfig(propConfig);
    setShowConfigModal(false);
  };

  const _saveConfig = () => {
    ConfigurationsActions.update(CONTEXT_COLLECTOR_CONFIG, config).then(() => {
      _closeModal();
    });
  };

  return (
      <div>
        <h3>NetUSE Context Collector Configuration</h3>

        <p>
          Configuration for the Context Collector.
        </p>

        <dl className="deflist">
          <dt>Configured by:</dt>
          <dd>{config.load_configuration_from_file ? 'File' : 'Cluster Config'}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={_openModal}>Configure</Button>
        </IfPermitted>

        <BootstrapModalForm show={showConfigModal}
                            title="NetUSE Context Collector Configuration"
                            onSubmitForm={_saveConfig}
                            onCancel={_resetConfig}
                            submitButtonText="Save"
                            bsSize="large">
          <fieldset>

            <DataTable id="configurations_list"
                       className="table-hover"
                       noDataText="No Collections configured"
                       headers={headers}
                       headerCellFormatter={(header) => <th>{header}</th>}
                       rows={config.collection_configurations}
                       disabled={config.load_configuration_from_file}
                       dataRowFormatter={rowFormatter} />

              <Button
                    bsSize="xs"
                    bsStyle="primary"
                    onClick={_onContextCollectionAdd}
                    disabled={config.load_configuration_from_file}
                    >
                    Add Collection
              </Button>

            <Input id="load_configuration_from_file"
                   name="load_configuration_from_file"
                   type="checkbox"
                   label="Load From File"
                   help="Should the configuration be read from file"
                   checked={config.load_configuration_from_file}
                   onChange={_onCheckboxClick('load_configuration_from_file')} />


            <Input type="text"
                    id="collection_configurations_path"
                    label="File Path"
                    help="Configure the file path"
                    name="collection_configurations_path"
                    value={config.collection_configurations_path}
                    onChange={_onUpdate('collection_configurations_path')}
                    disabled={!config.load_configuration_from_file}
                    />

          </fieldset>
        </BootstrapModalForm>
      </div>
    );
};

export default NetuseContextCollectorConfig;
