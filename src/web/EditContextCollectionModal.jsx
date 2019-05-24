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
import React from 'react';


import { BootstrapModalConfirm, Input } from 'components/bootstrap';
import { Panel, Button } from 'components/bootstrap';

import SortableSelect from 'views/components/aggregationbuilder/SortableSelect';
import CustomPropTypes from 'views/components/CustomPropTypes';

import FormUtils from 'util/FormsUtils';
import { defaultCompare } from 'logic/DefaultCompare';


import styled from 'styled-components';


const ValueComponent = styled.span`
  padding: 2px 5px;
`;

class EditContextCollectionModal extends React.Component {
  static propTypes = {
    name: PropTypes.string,
    type: PropTypes.string,
    key_fields: PropTypes.string,
    value_fields: PropTypes.array,
    timeout: PropTypes.int,
    drop_incomplete: PropTypes.bool,
    create: PropTypes.bool,
    update: PropTypes.func.isRequired,
    fields: CustomPropTypes.FieldListType.isRequired,
  };

  static defaultProps = {
    name: '',
    type: 'collection',
    key_fields: [],
    value_fields: [],
    timeout: 10,
    create: false,
    drop_incomplete: false,
  };

  constructor(props) {
    super(props);
    this.state = {
        name: props.name,
        type: props.type,
        key_fields: props.key_fields,
        timeout: props.timeout,
        value_fields: props.value_fields,
        drop_incomplete: props.drop_incomplete,
        enabled: true,
	showModal: false,
    };
  };

  openModal = () => {
    this.setState({showModal: true});
  };

    _onChangeValueFields = (value) => {
          let update = {value_fields: value};
          this.setState(update);
    };

    _onChangeKeyFields = (value) => {
          let update = {key_fields: value};
          this.setState(update);
    };

    _onChangeEvent = (field) => {
      return (e) => {
          let update = {}
          update[field] = FormUtils.getValueFromInput(e.target);
          this.setState(update);
      }
    }

  _onCancel = () => {
    this.setState({showModal: false});
  };

  _onConfirm = () => {
    this.setState({showModal: false});
    let tmp = this.state
    delete tmp['showModal']
    this.props.update(tmp);
  };

  render() {
    const selectedFieldsForSelect = this.state.value_fields.map((fieldName) => ({ field: fieldName }));
    const selectedFieldsForSelect2 = this.state.key_fields.map((fieldName) => ({ field: fieldName }));
    const availableSortedFields = this.props.fields
      .map((fieldType) => fieldType.name)
      .map((fieldName) => ({ label: fieldName, value: fieldName }))
      .valueSeq()
      .toJS()
      .sort((v1, v2) => defaultCompare(v1.label, v2.label));


    let triggerButtonContent;
    if (this.props.create) {
      triggerButtonContent = 'Create pattern';
    } else {
      triggerButtonContent = <span>Edit</span>;
    }

    return (
      <span>
        <Button onClick={this.openModal}
                disabled={this.props.disabled}
                style={{ marginRight: 5 }}
                bsStyle={this.props.create ? 'success' : 'info'}
                bsSize={this.props.create ? undefined : 'xs'}>
          {triggerButtonContent}
        </Button>
        <BootstrapModalConfirm show={this.state.showModal}
                            title={`${this.props.create ? 'this.props.Create' : 'Edit'} Context Collection ${name}`}
                            bsSize="large"
                            onConfirm={this._onConfirm}
                            onCancel={this._onCancel}
                            submitButtonText="Ok">
          <fieldset>
            <Input type="text"
                   id="collection_name"
                   label="Name"
                   onChange={this._onChangeEvent('name')}
                   value={this.state.name}
                   required />

            <label htmlFor="input-value-fields2">Key fields</label>
            <SortableSelect options={availableSortedFields}
                            onChange={this._onChangeKeyFields}
                            value={selectedFieldsForSelect2}
                            valueComponent={ValueComponent}
                            inputId="input-value-fields2"
                            allowOptionCreation={false} />

            <Input type="text"
                   id="timeout"
                   label="Timeout"
                   onChange={this._onChangeEvent('timeout')}
                   value={this.state.timeout}
                   required />

            <label htmlFor="input-value-fields">Value fields</label>
            <SortableSelect options={availableSortedFields}
                            onChange={this._onChangeValueFields}
                            value={selectedFieldsForSelect}
                            valueComponent={ValueComponent}
                            inputId="input-value-fields"
                            allowOptionCreation={false} />

             <Input type="checkbox"
                id="drop_incomplete"
                label="Drop incomplete Collections on timeout"
                onChange={this._onChangeEvent('drop_incomplete')}
                defaultChecked={this.state.drop_incomplete}
                />
          </fieldset>
        </BootstrapModalConfirm>
      </span>
    );
  }
}

export default EditContextCollectionModal;

